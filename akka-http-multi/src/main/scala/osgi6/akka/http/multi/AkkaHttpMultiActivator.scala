package osgi6.akka.http.multi

import javax.servlet.http.HttpServletRequest

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import org.osgi.framework.BundleContext
import osgi6.actor.ActorSystemActivator
import osgi6.akka.stream.AkkaStreamActivator
import osgi6.common.{AsyncActivator, BaseActivator}
import osgi6.lib.multi.MultiApiActivator
import osgi6.multi.api.MultiApi

import scala.concurrent.ExecutionContext

/**
  * Created by pappmar on 05/07/2016.
  */
import osgi6.akka.http.multi.AkkaHttpMultiActivator._

class AkkaHttpMultiActivator(
  starter: Start,
  filter: HttpServletRequest => Boolean = _ => true,
  classLoader: Option[ClassLoader] = None,
  config : Config = ConfigFactory.empty()
) extends AkkaStreamActivator(
  { ctx =>
    import ctx._

    AkkaHttpMultiActivator.activate(
      starter(ctx),
      filter
    )
  },
  classLoader = classLoader,
  config = config
)


object AkkaHttpMultiActivator {

  type Input = AkkaStreamActivator.Input
  type Run = (Route, AsyncActivator.Stop)
  type Start = Input => Run

  def activate(
    run: Run,
    filter: HttpServletRequest => Boolean = _ => true
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) = {
    import actorSystem.dispatcher

    val (route, routeStop) = run

    AsyncActivator.stops(
      MultiApiActivator.activate(
        AkkaHttpMultiApiHandler(route, filter)
      ),
      routeStop
    )

  }

}
