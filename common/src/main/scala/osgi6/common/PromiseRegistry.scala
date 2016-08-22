package osgi6.common

import scala.collection.JavaConversions._
import scala.collection.immutable._

/**
  * Created by martonpapp on 07/07/16.
  */
class PromiseRegistry[Value, Listener <: AnyRef, Registration](
  notify: (Listener, Value) => Unit,
  unreg : (() => Unit) => Registration
)(implicit ev: Null <:< Listener) {

  private var value : Option[Value] = None

  private var listeners = Seq[Listener]()

  def listen(handler: Listener) : Registration = synchronized {
    unreg(
      value
        .map({ v =>
          notify(handler, v)

          () => ()
        })
        .getOrElse({
          listeners = handler +: listeners

          { () =>
            synchronized {
              listeners = listeners diff Seq(handler)
            }
          }
        })
    )
  }

  def set(v: Value) : Unit = synchronized {
    value = Some(v)
    listeners.foreach({ l =>
      notify(l, v)
    })
    listeners = Seq()
  }

}
