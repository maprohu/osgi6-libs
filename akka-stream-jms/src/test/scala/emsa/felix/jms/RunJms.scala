package emsa.felix.jms

import javax.jms.{Message, MessageListener, Session, TextMessage}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import org.apache.activemq.command.ActiveMQQueue
import org.apache.activemq.{ActiveMQConnectionConsumer, ActiveMQConnectionFactory}
import osgi6.akka.stream.jms.JmsSink

import scala.concurrent.Future
import scala.io.StdIn

/**
  * Created by martonpapp on 03/07/16.
  */
object RunJms {

  def main(args: Array[String]) {

    implicit val actorSystem = ActorSystem()
    import actorSystem.dispatcher
    implicit val materializer = ActorMaterializer()

    val cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false")
    val dest = new ActiveMQQueue("queueueueue")


    val conn = cf.createConnection()
    conn.start()

    val session = conn
      .createSession(false, Session.AUTO_ACKNOWLEDGE)

    val max = 10000

    val consumer = session.createConsumer(dest)
    consumer.setMessageListener(new MessageListener {
      var count = 0
      override def onMessage(message: Message): Unit = {
        println(message.asInstanceOf[TextMessage].getText)
        this.synchronized {
          count += 1
          if (count == max) {
            consumer.close()
            session.close()
            conn.stop()
            conn.close()
          }
        }

      }
    })
    session.run()

    Source(1 to max)
      .map(_.toString)
      .toMat(
        JmsSink.text(
          4,
          () => Future.successful((cf, dest))
        )
      )(Keep.right)
      .run()
      .onComplete({ res =>
        println(res)
      })


    StdIn.readLine()



  }

}
