package osgi6.common

import org.osgi.framework.{BundleActivator, BundleContext}
import osgi6.common.BaseActivator.{Input, Start, Stop}

/**
  * Created by martonpapp on 23/08/16.
  */
class SimpleActivator(starter: Start) extends BundleActivator {

  var stop : Stop = () => ()

  override def start(context: BundleContext): Unit = {
    stop = starter(Input(context))
  }

  override def stop(context: BundleContext): Unit = {
    stop()
  }

}
