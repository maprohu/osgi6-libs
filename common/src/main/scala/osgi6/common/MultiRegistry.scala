package osgi6.common

import scala.collection.immutable._

/**
  * Created by martonpapp on 04/07/16.
  */
class MultiRegistry[T] {

  private var items = Seq[T]()

  def register(item: T) : Unit = synchronized {
    items = item +: items
  }

  def unregister(item: T) : Unit = synchronized {
    items = items diff Seq(item)
  }

  def iterate : java.util.Enumeration[T] = synchronized {
    import scala.collection.JavaConversions._

    items.toIterator
  }

}
