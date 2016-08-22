package osgi6.lib.multi

import javax.servlet.http.HttpServletRequest

import org.osgi.framework.BundleContext
import osgi6.common.{AsyncActivator, HasBundleContext}
import osgi6.multi.api.{Context, MultiApi}

/**
  * Created by pappmar on 05/07/2016.
  */
import osgi6.lib.multi.MultiApiActivator._

class MultiApiActivator(starter: Start) extends AsyncActivator({ ctx =>
  activate(starter(ctx))
})

object MultiApiActivator {

  type Start = HasBundleContext => Run
  type Run = (MultiApi.Handler, AsyncActivator.Stop)

  def activate(
    run: Run
  ) : AsyncActivator.Stop = {
    val (handler, stop) = run

    val reg = MultiApi.registry.register(handler)

    { () =>
      reg.remove

      stop()
    }

  }

  def extractPath(
    context: Context,
    req: HttpServletRequest
  ) = {

    val rootPath = context.rootPath
    val requestUri = req.getServletPath + Option(req.getPathInfo).getOrElse("")

    val (root, info) = requestUri.splitAt(rootPath.length)

    val servletPath = Option(info)

    if (root != rootPath) {
      None
    } else {
      servletPath
    }
  }

}
