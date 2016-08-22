package osgi6.akka.stream.jms

import javax.jms._

import akka.actor.ActorSystem
import akka.stream.hack.HackSource
import akka.stream.scaladsl.{Keep, Source}
import maprohu.scalaext.common.{Cancel, Stateful}
import osgi6.actor.Retry
import osgi6.akka.stream.Stages
import osgi6.akka.stream.Stages.MapMat

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by pappmar on 14/07/2016.
  */
object JmsSource {

  def listen(
    connecter : JmsSink.Connecter
  )(implicit
    actorSystem: ActorSystem
  ) = {
    import actorSystem.dispatcher

    case class State(
      connection: Connection,
      consumer: MessageConsumer
    ) {
      val promise = Promise[Unit]()

      def close() : Unit = {
        Try(connection.close())
        promise.trySuccess()
      }
    }

    Source.repeat(())
      .viaMat(
        MapMat({ () =>

          @volatile var state : Future[State] = null
          val cancels = Stateful.cancels

          val receive = { () =>
              val stOpt : Option[Future[State]] = if (state == null) {
                cancels.addValue({ () =>
                  state = connecter().map({
                    case (connectionFactory, destination) =>
                      val connection = connectionFactory.createConnection()
                      val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                      val consumer = session.createConsumer(destination)
                      connection.start()
                      State(connection, consumer)
                  })

                  val c = Cancel(
                    () => state.foreach(_.close()),
                    state.flatMap(_.promise.future)
                  )

                  (c, Some(state))
                }).getOrElse(
                  None
                )
              } else {
                Some(state)
              }

              stOpt.map({ st =>
                st
                  .map[Option[Message]]({ s =>
                    try {
                      Some(s.consumer.receive())
                    } catch {
                      case ex: Throwable =>
                        s.close()
                        throw ex
                    }
                  })
              }).getOrElse(
                Future.successful(None)
              )

          }

          val retryReceive = Retry(
            receive,
            cancels
          )

          val stopper = { () =>
            cancels.cancel.perform
          }

          (retryReceive, stopper)

        })( (mat, _) => mat._1 )
      )({ (_, mat) => mat._2 })
      .mapAsync(1)({ s =>
        s()
      })
      .takeWhile(_.isDefined)
      .map(_.get)
      .viaMat(
        Stages.terminationWatcher
      )({ (termJms, term) =>
        term.onComplete({ _ =>
          termJms()
        })

        { () =>
          termJms()
          ()
        }

      })












  }

}
