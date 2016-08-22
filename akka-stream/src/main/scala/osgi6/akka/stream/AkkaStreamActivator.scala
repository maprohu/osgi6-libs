package osgi6.akka.stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import org.osgi.framework.BundleContext
import osgi6.actor.{ActorSystemActivator, HasActorSystem}
import osgi6.common.{AsyncActivator, BaseActivator, HasBundleContext}

/**
  * Created by pappmar on 05/07/2016.
  */
import osgi6.akka.stream.AkkaStreamActivator._

class AkkaStreamActivator(
  starter: Start,
  classLoader: Option[ClassLoader] = None,
  config : Config = ConfigFactory.empty()
) extends BaseActivator({ ctx =>
  activate(
    ctx.bundleContext,
    starter,
    classLoader,
    config
  )
})

trait HasMaterializer {
  implicit val materializer : Materializer
}

object AkkaStreamActivator {

  type Input = HasMaterializer with HasActorSystem with HasBundleContext
  type Start = Input => AsyncActivator.Stop

  case class Holder(
    bundleContext: BundleContext,
    actorSystem: ActorSystem,
    materializer: Materializer
  ) extends HasMaterializer with HasActorSystem with HasBundleContext

  def activate(
    ctx : BundleContext,
    starter: Start,
    classLoader: Option[ClassLoader] = None,
    config : Config = ConfigFactory.empty()
  ) : BaseActivator.Stop = {

    ActorSystemActivator.activate(
      ctx,
      starter = { ctx =>
        import ctx.actorSystem
        val materializer = Materializers.resume

        starter(Holder(
          ctx.bundleContext,
          ctx.actorSystem,
          materializer
        ))
      },
      classLoader = classLoader,
      config = config

    )

  }

}
