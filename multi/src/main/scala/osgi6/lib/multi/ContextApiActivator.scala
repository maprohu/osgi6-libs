package osgi6.lib.multi

import maprohu.scalaext.common.{Cancel, Stateful}
import org.osgi.framework.BundleContext
import osgi6.common.AsyncActivator
import osgi6.multi.api.{Context, ContextApi}
import osgi6.multi.api.ContextApi.Handler

import scala.concurrent.ExecutionContext

/**
  * Created by pappmar on 13/07/2016.
  */
trait HasApiContext {

  implicit val apiContext : Option[Context]

}

object ContextApiActivator {


  type Input = HasApiContext
  type Start = Input => AsyncActivator.Stop

  case class Holder(
    apiContext: Option[Context]
  ) extends HasApiContext

  def activateNonNull(
    starter: Context => AsyncActivator.Stop
  )(implicit
    executionContext: ExecutionContext
  ) = {
    activate(
      { hasCtx =>
        hasCtx.apiContext.map({ apiCtx =>
          starter(apiCtx)
        }).getOrElse(
          AsyncActivator.Noop
        )
      }
    )
  }

  def activate(
    starter: Start
  )(implicit
    executionContext: ExecutionContext
  ) = {
    val cancel = Stateful.cancels

    val reg = ContextApi.registry.listen(new Handler {
      override def dispatch(apiCtx: Context): Unit = {
        cancel.add({ () =>
          Cancel(
            starter(
              Holder(
                Option(apiCtx)
              )
            )
          )
        })
      }
    })

    { () =>
      reg.remove
      cancel.cancel.perform
    }

  }

}
