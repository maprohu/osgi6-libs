package osgi6.akka.slf4j

import com.typesafe.config.ConfigFactory

/**
  * Created by pappmar on 07/07/2016.
  */
object AkkaSlf4j {

  def config = ConfigFactory.parseString(
    """
      |akka {
      |  loggers = ["akka.event.slf4j.Slf4jLogger"]
      |  loglevel = "DEBUG"
      |  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
      |}
    """.stripMargin
  )

}
