package osgi6.common

import org.osgi.framework.{BundleActivator, BundleContext}

import scala.concurrent.Future

/**
  * Created by martonpapp on 04/07/16.
  */
import BaseActivator._

class BaseActivator(starter: Start) extends BundleActivator { self =>
  var stop : Stop = () => ()

  override def start(context: BundleContext): Unit = {
    stop = HygienicThread.execute {
      Thread.currentThread().setContextClassLoader(self.getClass.getClassLoader)
      starter(Input(context))
    }
  }

  override def stop(context: BundleContext): Unit = {
    try {
      HygienicThread.execute {
        Thread.currentThread().setContextClassLoader(self.getClass.getClassLoader)
        stop()
      }
    } finally {
      stop = null
    }
  }

}

object BaseActivator {
  type Stop = () => Unit
  type Start = HasBundleContext => Stop

  case class Input(
    bundleContext: BundleContext
  ) extends HasBundleContext

}

trait HasBundleContext {
  implicit val bundleContext : BundleContext
}
