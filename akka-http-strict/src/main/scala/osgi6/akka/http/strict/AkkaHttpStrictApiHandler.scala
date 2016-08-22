package osgi6.akka.http.strict

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import maprohu.scalaext.common.Stateful
import osgi6.common.AsyncActivator
import osgi6.strict.api.StrictApi

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by pappmar on 05/07/2016.
  */
object AkkaHttpStrictApiHandler {

  def apply(
    route: Route
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) = {
    import actorSystem.dispatcher

    val (processor, cancel) = AkkaHttpStrict.processor(route)

    val handler =
      new StrictApi.Handler {
        override def dispatch(request: StrictApi.Request, callback: StrictApi.Callback): Unit = {
          processor(request)
            .onComplete({
              case Success(None) | Failure(_) =>
                callback.next
              case Success(Some(res)) =>
                callback.handled(res)
            })

        }
      }

    (handler, cancel)

  }



}

object AkkaHttpStrict {

  def wrapRequest(req: StrictApi.Request) : HttpRequest = {

    HttpRequest(
      method = HttpMethods.getForKey(req.method).get,
      uri = Uri(req.requestUri).copy(rawQueryString = Option(req.queryString)),
      headers = (
        for {
          (headerName, values) <- req.headers
          value <- values
        } yield {
          HttpHeader.parse(headerName, value).asInstanceOf[Ok].header
        }
      )(collection.breakOut),
      entity = HttpEntity(
        contentType =
          Option(req.contentType)
            .map(ct => ContentType.parse(ct).right.get)
            .getOrElse(ContentTypes.`application/octet-stream`),
        data =
          Source(req.content.to[collection.immutable.Iterable])
            .map(ByteString(_))

      ),
      protocol = HttpProtocols.getForKey(req.protocol).get
    )

  }

  def parseResponse(
    res: HttpResponse
  )(implicit
    materializer: Materializer,
    executionContext: ExecutionContext
  ) : Future[StrictApi.Response] = {
    for {
      bytes <- res.entity.dataBytes
        .map(_.toArray)
        .runWith(
          Sink.seq
        )
    } yield {
      new StrictApi.Response {
        override def content: java.lang.Iterable[Array[Byte]] = bytes

        override def status: Int = res.status.intValue()

        override def contentType: String = res.entity.contentType.value

        override def headers: java.util.Map[String, java.lang.Iterable[String]] =
          res.headers.groupBy(_.name()).map({
            case (name, headers) =>
              name -> asJavaIterable(headers.map(_.value()))
          })

      }
    }


  }

  type RequestProcessorFuture = Future[Option[StrictApi.Response]]
  type RequestProcessor = StrictApi.Request => RequestProcessorFuture

  val NotHandled = StatusCodes.custom(
    1007,
    "not handled",
    "not handled",
    false,
    false
  )

  val notHandledResponse =
    HttpResponse(
      status = NotHandled,
      entity = HttpEntity.empty(ContentTypes.`text/plain(UTF-8)`)
    )

  def processor(
    route: Route
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) : (RequestProcessor, AsyncActivator.Stop) = {
    import actorSystem.dispatcher

    @volatile var cancelled = false

    val routeHandler = Route.asyncHandler(
      pathPrefix( Segment ) { _ =>
        route ~ complete(
          notHandledResponse
        )
      }
    )

    val futures = Stateful.futures[Any]

    val requestProcessor : RequestProcessor = { req =>
      if (cancelled) {
        Future.successful(None)
      } else {
        val future =
          routeHandler(wrapRequest(req))
            .flatMap({ httpResponse =>
              if (httpResponse.status == NotHandled) {
                Future.successful(None)
              } else {
                parseResponse(httpResponse).map(Some(_))
              }
            })

        futures.add(future)

        future
      }
    }


    val cancel : AsyncActivator.Stop = () => {
      cancelled = true
      futures.future.recover({ case o => o })
    }

    (requestProcessor, cancel)
  }

}
