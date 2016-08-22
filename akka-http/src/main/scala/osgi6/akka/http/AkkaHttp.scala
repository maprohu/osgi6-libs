package osgi6.akka.http

import java.io.{File, FileInputStream}

import akka.http.scaladsl.model.headers.EntityTag
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.CacheConditionDirectives._
import akka.stream.{ActorAttributes, Materializer}
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import osgi6.akka.stream.IOStreams

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by pappmar on 14/07/2016.
  */
object AkkaHttp {



  def toStrict[T <: HttpMessage]()(implicit
    materializer: Materializer,
    executionContext: ExecutionContext,
    timeout: Timeout = Timeout(1.minute)
  ) : T => Future[T] = { req =>
    req.entity.toStrict(timeout.duration).map({ strict =>
      req.withEntity(strict).asInstanceOf[T]
    })
  }

  def toString[T <: HttpMessage]()(implicit
    materializer: Materializer,
    executionContext: ExecutionContext,
    timeout: Timeout = Timeout(1.minute)
  ) : T => Future[T] = { req =>
    req.entity.toStrict(timeout.duration).map({ strict =>
      req.withEntity(strict.data.utf8String).asInstanceOf[T]
    })
  }

  def serveDirectory(dir: File)(implicit resolver: ContentTypeResolver) : Route = {
    import akka.http.scaladsl.server.Directives._

    path(Segments) { segments =>
      val file = segments.foldLeft(dir)((f, s) => new File(f, s))

      serveFile(file)
    }

  }

  private def conditionalFor(length: Long, lastModified: Long): Directive0 =
    extractSettings.flatMap(settings ⇒
      if (settings.fileGetConditional) {
        val tag = java.lang.Long.toHexString(lastModified ^ java.lang.Long.reverse(length))
        val lastModifiedDateTime = DateTime(math.min(lastModified, System.currentTimeMillis))
        conditional(EntityTag(tag), lastModifiedDateTime)
      } else pass)

  def serveFile(file: File)(implicit resolver: ContentTypeResolver) = {
    import akka.http.scaladsl.server.Directives._

    if (file.exists() && file.isFile) {
      conditionalFor(file.length(), file.lastModified()) {
        complete(
          HttpResponse(
            entity =
              HttpEntity.Default(
                resolver(file.getName),
                file.length,
                IOStreams.source(() => new FileInputStream(file))
              )
          )
        )
      }
    } else {
      reject
    }

  }


}

object Implicits {

  implicit def segmentsToPathMatcher(segments: TraversableOnce[String]) : PathMatcher0 = {
    if (segments.isEmpty) {
      PathMatchers.Neutral
    } else {
      import akka.http.scaladsl.server.Directives._
      segments.map(segmentStringToPathMatcher).reduce(_ / _)
    }
  }


}
