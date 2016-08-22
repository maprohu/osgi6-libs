package akka.stream.hack

import java.io.InputStream

import akka.stream.impl.{AcknowledgeSource, ErrorPublisher, SourceModule}
import akka.stream.impl.Stages.DefaultAttributes
import akka.stream.impl.StreamLayout.Module
import akka.stream.impl.io.{InputStreamPublisher, InputStreamSource}
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, _}
import akka.util.ByteString
import org.reactivestreams.Publisher

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

/**
  * Created by pappmar on 11/07/2016.
  */
object HackSource {

  def queue[T](
    bufferSize: Int,
    overflowStrategy: OverflowStrategy,
    timeout: FiniteDuration = 5.seconds
  ): Source[T, SourceQueue[T]] = {
    require(bufferSize >= 0, "bufferSize must be greater than or equal to 0")
    new Source(new AcknowledgeSource(
      bufferSize,
      overflowStrategy,
      DefaultAttributes.acknowledgeSource,
      Source.shape("AcknowledgeSource"),
      timeout
    ))
  }

}

