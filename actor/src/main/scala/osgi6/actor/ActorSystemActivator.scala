package osgi6.actor

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.osgi.framework.BundleContext
import osgi6.common.{AsyncActivator, BaseActivator, HasBundleContext}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by pappmar on 05/07/2016.
  */
import ActorSystemActivator._

class ActorSystemActivator(
  starter : Start,
  classLoader: Option[ClassLoader] = None,
  name: Option[String] = None,
  config: Config = ConfigFactory.empty(),
  shutdownTimeout: FiniteDuration = 30.seconds
) extends BaseActivator({ ctx =>
  activate(
    ctx.bundleContext,
    starter,
    classLoader,
    name,
    config,
    shutdownTimeout
  )
})

trait HasActorSystem {
  implicit val actorSystem : ActorSystem
}

object ActorSystemActivator {
  type Input = HasActorSystem with HasBundleContext
  type Start = Input => AsyncActivator.Stop

  case class Holder(
    bundleContext: BundleContext,
    actorSystem: ActorSystem
  ) extends HasActorSystem with HasBundleContext

  def activate(
    ctx: BundleContext,
    starter : Start,
    classLoader: Option[ClassLoader] = None,
    name: Option[String] = None,
    config: Config = ConfigFactory.empty(),
    shutdownTimeout: FiniteDuration = 30.seconds
  ) : BaseActivator.Stop = {
    val actorSystem = create(
      name.getOrElse(ctx.getBundle.getSymbolicName.collect({
        case ch if Character.isLetterOrDigit(ch) => ch
        case other => '_'
      })).dropWhile(ch => !Character.isLetterOrDigit(ch)),
      config,
      classLoader
    )

    val stop = starter(Holder(ctx, actorSystem))

    () => {
      Await.result(stop(), shutdownTimeout)
      actorSystem.shutdown()
      actorSystem.awaitTermination(shutdownTimeout)

      // i don't know why this is here
      Thread.sleep(500)
    }
  }

  val forcedConfig = ConfigFactory.parseString(
    """
      |akka {
      |  loglevel = "DEBUG"
      |  jvm-exit-on-fatal-error = false
      |  actor {
      |    default-dispatcher {
      |      executor = "thread-pool-executor"
      |    }
      |  }
      |}
    """.stripMargin
  )

  def create(
    name: String,
    config: Config = ConfigFactory.empty(),
    classLoader: Option[ClassLoader] = None
  ) : ActorSystem = {

    ActorSystem(
      name,
      Some(
        forcedConfig.withFallback(
          config.withFallback(
            classLoader.map({ cl =>
              ConfigFactory.load(cl)
            }).getOrElse(
              ConfigFactory.load()
            )
          )
        )
      ),
      classLoader
    )

  }
}
