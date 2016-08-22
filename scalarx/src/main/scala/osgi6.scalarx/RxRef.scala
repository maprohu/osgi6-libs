package osgi6.scalarx

import rx.Ctx.Owner.Unsafe
import rx._
import scala.collection.immutable._

/**
  * Created by pappmar on 25/11/2015.
  */
trait RxRef[T] {
  rxRefThis =>

  private val refs = Var(Seq[Rx[Option[T]]]())
  val ref : Rx[Option[T]] = Rx.unsafe(refs().headOption.flatMap(_()))

  def register(value: Rx[Option[T]]) : RxRegistration = this.synchronized {
    refs() = value +: refs.now

    new RxRegistration {
      override def unregister(): Unit = {
        rxRefThis.synchronized {
          refs() = refs.now diff Seq(value)
        }
        value.kill()
      }
    }
  }

}

class RxSeq[T, V](combine: Seq[T] => V) {
  rxRefThis =>

  import Unsafe.Unsafe

  private val refs = Var(Seq[T]())
  val ref : Rx[V] = refs.map(combine)

  def register(value: T*) : RxRegistration = this.synchronized {
    refs() = value.to[Seq] ++ refs.now

    new RxRegistration {
      override def unregister(): Unit = {
        rxRefThis.synchronized {
          refs() = refs.now diff value
        }
      }
    }
  }

}

trait RxRegistration {
  def unregister() : Unit
}
