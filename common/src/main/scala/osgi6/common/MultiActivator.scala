package osgi6.common

import org.osgi.framework.BundleActivator

import scala.util.Try

/**
  * Created by martonpapp on 05/07/16.
  */
class MultiActivator(activators: BundleActivator*) extends BaseActivator({ ctx =>

  activators.foldLeft(() => ())({ (stopper, activator) =>
    Try(activator.start(ctx.bundleContext))
      .map(_ => () => {
        Try(activator.stop(ctx.bundleContext))
        stopper()
      })
      .recover({
        case ex =>
          Try(stopper())
          throw ex
      })
      .get
  })

})
