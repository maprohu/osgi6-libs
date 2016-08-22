package osgi6.akka.http.multi

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import osgi6.akka.http.multi.AkkaHttpServlet.RequestProcessor
import osgi6.common.AsyncActivator
import osgi6.multi.api.MultiApi

import scala.concurrent.ExecutionContext

/**
  * Created by pappmar on 05/07/2016.
  */
object AkkaHttpMultiApiHandler {

  def apply(
    route: Route,
    filter: HttpServletRequest => Boolean = _ => true
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) : (MultiApi.Handler, AsyncActivator.Stop) = {
    import actorSystem.dispatcher

    val (processor, cancel) = AkkaHttpServlet.processor(route, filter)

    val handler =
      new Handler(processor)

    (handler, cancel)

  }

  class Handler(val processor: RequestProcessor)(implicit
    executionContext: ExecutionContext
  ) extends MultiApi.Handler {
    override def dispatch(request: HttpServletRequest, response: HttpServletResponse, callback: MultiApi.Callback): Unit = {
      processor.process(request, response)
        .foreach(callback.handled)
    }
  }

}


