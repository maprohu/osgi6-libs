package osgi6.akka.stream.jms

import javax.jms._

import akka.stream.scaladsl.{Flow, Keep, Sink}
import maprohu.scalaext.common.Stateful
import osgi6.akka.stream.Stages.MapMat

import scala.concurrent.{ExecutionContext, Future}
import collection.immutable._
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Created by martonpapp on 03/07/16.
  */
object JmsSink {

  type Connecter = () => Future[(ConnectionFactory, Destination)]

  trait Pool {
    def perform(send: Session => Message) : Future[Unit]
    def close : Unit
  }

  def pool(
    connect : Connecter
  )(implicit
    executionContext: ExecutionContext
  ) : Pool = {
    new Pool {
      case class Conn(
        connection: Connection,
        dest: Destination,
        session: Session,
        producer: MessageProducer
      ) {
        def perform(send: Session => Message) : Unit = {
          producer.send(send(session))
        }

        def close = {
          Try(producer.close())
          Try(session.close())
          Try(connection.close())
        }

      }

      val pool = Stateful(List[Conn]())


      override def perform(send: (Session) => Message): Future[Unit] = Future {
        def do1(conn: Conn) = {
          try {
            conn.perform(send)
            pool.update(list => Some(conn +: list))
          } catch {
            case NonFatal(_) =>
              conn.close
          }
        }

        pool.transform({ list =>

          if (list.isEmpty) {
            val fut = for {
              (cf, dest) <- connect()
            } yield {
              val connection = cf.createConnection()
              val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
              val producer = session.createProducer(dest)

              val conn =
                Conn(
                  connection,
                  dest,
                  session,
                  producer
                )

              do1(conn)
            }

            (fut, list)
          } else {
            (Future(do1(list.head)), list.tail)
          }

        })



      }


      override def close: Unit = {
        pool.transform({ list =>
          list.foreach(_.close)
          ((), List())
        })

      }
    }

  }

  def text(
    parallelism: Int,
    connect : Connecter
  )(implicit
    executionContext: ExecutionContext
  ) = apply[String](
    parallelism,
    connect,
    (msg, session) => {
      session.createTextMessage(msg)
    }
  )

  type TextHeaders = (String, Map[String, String])

  def textHeaders(
    parallelism: Int,
    connect : Connecter
  )(implicit
    executionContext: ExecutionContext
  ) : Sink[TextHeaders, Future[Unit]] = apply[TextHeaders](
    parallelism,
    connect,
    (msgHeader, session) => {
      val (msg, headers) = msgHeader
      val tm = session.createTextMessage(msg)
      headers.foreach({ case (k, v) =>
        tm.setStringProperty(k, v)
      })
      tm
    }
  )
  def apply[T](
    parallelism: Int,
    connect : Connecter,
    send: (T, Session) => Message
  )(implicit
    executionContext: ExecutionContext
  ) : Sink[T, Future[Unit]] = {
    Flow[T]
      .viaMat(
        MapMat(
          () => pool(connect)
        )(
          (pool, item) => (pool, item)
        )
      )(Keep.right)
      .toMat(
        Sink.foreachParallel(parallelism)({
          case (pool, item) =>
            pool.perform(session => send(item, session))
        })
      )(Keep.both)
      .mapMaterializedValue({ case (pool, done) =>
          done
            .andThen({ case _ => pool.close })
      })
  }

}
