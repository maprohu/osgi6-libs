package osgi6.common

import scala.collection.immutable._
import scala.collection.JavaConversions._

/**
  * Created by martonpapp on 07/07/16.
  */
class BaseRegistry[Handler <: AnyRef, Registration](
  unreg : (() => Unit) => Registration
)(implicit ev: Null <:< Handler) {

  var items = Seq[Handler]()

  def register(handler: Handler) : Registration = synchronized {
    items = handler +: items

    unreg(() => synchronized {
      items = items diff Seq(handler)
    })
  }

  def iterate : java.util.Enumeration[Handler] = synchronized {
    items.iterator
  }

  def first : Handler = synchronized {
    items.headOption.orNull
  }

}
