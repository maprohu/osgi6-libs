package osgi6.akka.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import org.reactivestreams.Publisher

import scala.concurrent.duration._
import scala.io.StdIn

/**
  * Created by pappmar on 07/07/2016.
  */
object RunFanout {

  def main(args: Array[String]) {

    implicit val actorSystem = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import actorSystem.dispatcher



    val pub =
      Source(Stream.from(1))
        .throttle(5, 1.second, 1, ThrottleMode.Shaping)
        .toMat(Sink.asPublisher(true))(Keep.right)
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
    val stop1 = printerSub("s1", pub)
    StdIn.readLine()

    println("sub2")
    val stop2 = printerSub("s2", pub)
    StdIn.readLine()

    println("stop1")
    stop1()
    StdIn.readLine()


  }

}
