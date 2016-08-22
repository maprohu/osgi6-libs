package osgi6.akka.stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.reactivestreams.Publisher

import scala.concurrent.duration._
import scala.io.StdIn

/**
  * Created by pappmar on 07/07/2016.
  */
object RunSplitStream {

  def main(args: Array[String]) {

    implicit val actorSystem = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import actorSystem.dispatcher



    val (sw, pub1, pub2) =
      Source(Stream.from(1))
        .throttle(5, 1.second, 1, ThrottleMode.Shaping)
        .toMat(Stages.switcher(
          Sink.asPublisher(false),
          Sink.asPublisher(false)
        ))(Keep.right)
//        .toMat(Sink.asPublisher(true))(Keep.right)
        .run()

    def printerSink(tag: String) =
      Flow[Any]
        .toMat(
          Sink.foreach(e => println(s"${tag}: ${e}"))
        )((_, fut) =>
          fut.onComplete(r => println(s"done: ${tag} - ${r}"))
        )

    def printerSub(tag: String, publisher: Publisher[_]) =
      Source.fromPublisher(publisher)
        .viaMat(
          Stages.stopper
        )(Keep.right)
        .to(printerSink(tag))
        .run()


    println("sub1")
    val stop1 = printerSub("s1", pub1)
    StdIn.readLine()

    println("sub2")
    val stop2 = printerSub("s2", pub2)
    StdIn.readLine()


    println("sw")
    sw()
    StdIn.readLine()


    println("stop1")
    stop1()
    StdIn.readLine()


  }

}
