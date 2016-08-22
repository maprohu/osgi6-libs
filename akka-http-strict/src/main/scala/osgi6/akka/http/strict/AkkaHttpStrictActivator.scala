package osgi6.akka.http.strict

import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import osgi6.actor.ActorSystemActivator
import osgi6.akka.stream.AkkaStreamActivator
import osgi6.common.{AsyncActivator, BaseActivator}
import osgi6.lib.strict.StrictApiActivator

/**
  * Created by pappmar on 05/07/2016.
  */
import osgi6.akka.http.strict.AkkaHttpStrictActivator._

class AkkaHttpStrictActivator(
  starter: Start,
  classLoader: Option[ClassLoader] = None,
  config : Config = ConfigFactory.empty()
) extends AkkaStreamActivator(
  { ctx =>
    import ctx._
    import actorSystem.dispatcher

    val (route, routeStop) = starter(ctx)

    AsyncActivator.stops(
      StrictApiActivator.activate(
        AkkaHttpStrictApiHandler(route)
      ),
      routeStop
    )

  },
  classLoader = classLoader,
  config = config
)


object AkkaHttpStrictActivator {

  type Input = ActorSystemActivator.Input
  type Run = (Route, AsyncActivator.Stop)
  type Start = Input => Run



}
