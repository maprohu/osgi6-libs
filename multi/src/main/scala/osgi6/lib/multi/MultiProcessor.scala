package osgi6.lib.multi

import java.io.File
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.slf4j.LoggerFactory
import osgi6.common.HttpTools
import osgi6.multi.api.{Context, MultiApiTrait}

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by martonpapp on 12/07/16.
  */
object MultiProcessor {

  val LOG = LoggerFactory.getLogger(MultiProcessor.getClass)

  def process(registry: MultiApiTrait.Registry, request: HttpServletRequest, response: HttpServletResponse): Future[Boolean] = {
    import scala.collection.JavaConversions._

    val promise = Promise[Boolean]()

    val handlers: Iterator[MultiApiTrait.Handler] = registry.iterate

    def processNext : Unit = {
      if (handlers.hasNext) {
        val handler = handlers.next()

        handler.dispatch(request, response, new MultiApiTrait.Callback {
          override def handled(result: Boolean): Unit = {
            if (result) {
              promise.success(true)
            } else {
              processNext
            }
          }
        })

      } else {
        promise.success(false)
      }

    }

    processNext

    promise.future

  }

  def processSync(registry: MultiApiTrait.Registry, request: HttpServletRequest, response: HttpServletResponse) = {
    HttpTools.preResponse(request, response)

    try {
      val processed = Await.result(
        MultiProcessor.process(registry, request, response),
        1.minute
      )

      if (!processed) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND)
      }

    } catch {
      case ex : Throwable =>
        LOG.error("error procesing request", ex)

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        response.getOutputStream.write("ERROR".getBytes())
    }

    Try(while (request.getInputStream.read() != -1) {})
    request.getInputStream.close()
    response.getOutputStream.flush()
    response.getOutputStream.close()

  }



}


case class DefaultContext(
  name: String = "test",
  data: File,
  log: File,
  debug: Boolean = true,
  stdout: Boolean = true,
  rootPath: String = "test",
  servletConfig: ServletConfig = null
) extends Context

object DefaultContext {
  def create(dir: File) = DefaultContext(
    data = new File(dir, "data"),
    log = new File(dir, "log.txt")
  )
}