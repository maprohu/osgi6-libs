package osgi6.lib.strict

import org.osgi.framework.BundleContext
import osgi6.common.{AsyncActivator, HasBundleContext}
import osgi6.strict.api.StrictApi

/**
  * Created by pappmar on 05/07/2016.
  */
import StrictApiActivator._
class StrictApiActivator(starter: Start) extends AsyncActivator({ ctx =>
  activate(starter(ctx))
})

object StrictApiActivator {

  type Start = HasBundleContext => Run
  type Run = (StrictApi.Handler, AsyncActivator.Stop)

  def activate(
    run: Run
  ) : AsyncActivator.Stop = {

    val (handler, stop) = run

    val reg = StrictApi.registry.register(handler)

    () => {
      reg.remove

      stop()
    }

  }

}
