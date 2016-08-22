package osgi6.common

import java.util.concurrent.{SynchronousQueue, ThreadPoolExecutor, TimeUnit}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * Created by martonpapp on 09/07/16.
  */
object HygienicThread {


  def execute[T]( task: => T, timeout: Duration = 10.minutes ) : T = {
    val promise = Promise[T]()
    val thread = new Thread() {
      override def run(): Unit = {
        try {
          promise.success(task)
        } catch {
          case ex : Throwable =>
            promise.failure(ex)

        }
      }
    }
    thread.setName("hygienic-" + Thread.currentThread().getName)
    thread.start()
    Await.result(promise.future, timeout)
  }

  def createExecutionContext = {
    val pool = new ThreadPoolExecutor(
      0,
      Integer.MAX_VALUE,
      10L, TimeUnit.SECONDS,
      new SynchronousQueue[Runnable]
    )
    val ec = ExecutionContext.fromExecutor(pool)

    val shut = () => {
      pool.shutdown()
      if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
        pool.shutdownNow()
        pool.awaitTermination(30, TimeUnit.SECONDS)
      }
      ()
    }

    (ec, shut)
  }

}

