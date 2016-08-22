package osgi6.akka.stream

import java.io.InputStream

import akka.stream.ActorAttributes
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.util.Try

/**
  * Created by martonpapp on 13/08/16.
  */
object IOStreams {

  def source( stream: () => InputStream ) : Source[ByteString, Unit] = {
    Source.fromIterator({ () =>
      val is = stream()
      Iterator.continually({
        val ba = Array.ofDim[Byte](1024 * 8)
        val count = is.read(ba)
        (ba, count)
      }).takeWhile({
        case (_, count) =>
          if (count != -1) {
            true
          } else {
            Try(is.close())
            false
          }
      }).map({
        case (bs, count) =>
          ByteString.fromArray(bs, 0, count)
      })
    }).withAttributes(
      ActorAttributes.dispatcher("akka.stream.default-blocking-io-dispatcher")
    )
  }

}
