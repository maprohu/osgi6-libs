package osgi6.common

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by martonpapp on 10/07/16.
  */
object AsyncTools {

  def foldSeq[T, V](zero: V, items: Seq[T])(fn: (T, V) => Future[V])(
    implicit executionContext: ExecutionContext
  ) : Future[V] = {
    items match {
      case head +: tail =>
        fn(head, zero).flatMap({ v =>
          foldSeq(v, tail)(fn)
        })
      case _ =>
        Future.successful(zero)
    }
  }

  def runSeq[T, R](items: Seq[T])(fn: T => Future[R])(
    implicit executionContext: ExecutionContext
  ) : Future[Seq[R]] = {
    foldSeq(Seq[R](), items)((item, seq) => fn(item).map(i => seq :+ i))
  }

}
