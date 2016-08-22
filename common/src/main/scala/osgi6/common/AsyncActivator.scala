package osgi6.common

import org.osgi.framework.BundleContext

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

/**
  * Created by pappmar on 05/07/2016.
  */
import AsyncActivator._
class AsyncActivator(starter: Start, timeout: Duration = 30.seconds) extends BaseActivator(toSync(starter, timeout))

object AsyncActivator {

  type Stop = () => Future[Any]
  type Start = HasBundleContext => Stop

  def toSync(starter: Start, timeout: Duration) : BaseActivator.Start = {
    ctx => {
      val stop = starter(ctx)

      () => {
        Await.result(stop(), timeout)
      }
    }
  }

  def stops(items: Stop*)(implicit executionContext: ExecutionContext) : Stop = {
    () => AsyncTools.runSeq(items)(_())
  }

  val Noop : AsyncActivator.Stop = () => Future.successful()

}
