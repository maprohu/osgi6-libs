package osgi6.actor

import akka.actor.ActorSystem
import maprohu.scalaext.common.{Cancel, StatefulCancels}
import osgi6.common.BaseActivator

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by pappmar on 14/07/2016.
  */
object Retry {

  def apply[T](
    producer: () => Future[Option[T]],
    cancels: StatefulCancels,
    retryTimeout: FiniteDuration = 3.seconds
  )(implicit
    actorSystem: ActorSystem
  ) : () => Future[Option[T]] = { () =>
    import actorSystem.dispatcher

    val promise = Promise[Option[T]]()

    def attempt() : Unit = {
      producer()
        .onComplete({
          case Success(v) =>
            promise.success(v)
          case Failure(ex) =>

            cancels.add({ () =>
              actorSystem.log.warning("failed attempt: {} - retrying in {}", ex, retryTimeout)

              val waitPromise = Promise[Unit]()

              val retryWait = actorSystem.scheduler.scheduleOnce(retryTimeout) {
                waitPromise.trySuccess(())
                attempt()
              }

              Cancel(
                () => {
                  retryWait.cancel()
                  waitPromise.trySuccess(())
                },
                waitPromise.future
              )
            }).getOrElse(
              promise.success(None)
            )
        })
    }

    attempt()

    promise.future
  }


}
