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
    stop = HygienicThread.execute(
      {
        starter(Input(context))
      },
      classLoader = Some(self.getClass.getClassLoader)
    )
  }

  override def stop(context: BundleContext): Unit = {
    try {
      HygienicThread.execute(
        {
          stop()
        },
        classLoader = Some(self.getClass.getClassLoader)
      )
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
