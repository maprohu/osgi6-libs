package osgi6.common

import java.lang.reflect.Field

/**
  * Created by pappmar on 26/08/2016.
  */
object ScalaThreadLocalCleaner {

  def cleanScalaThreadLocals : Int = {
    val thread: Thread = Thread.currentThread

    val threadLocalsField: Field = classOf[Thread].getDeclaredField("threadLocals")
    threadLocalsField.setAccessible(true)

    val threadLocalMapKlazz: Class[_] = Class.forName("java.lang.ThreadLocal$ThreadLocalMap")
    val tableField: Field = threadLocalMapKlazz.getDeclaredField("table")
    tableField.setAccessible(true)

    val table: Any = tableField.get(threadLocalsField.get(thread))
    val threadLocalCount: Int = java.lang.reflect.Array.getLength(table)

    (0 until threadLocalCount).count({ i =>
      val entry: Any = java.lang.reflect.Array.get(table, i)
      if (entry != null) {
        val valueField: Field = entry.getClass.getDeclaredField("value")
        valueField.setAccessible(true)
        val value: Any = valueField.get(entry)
        if (value != null && value.getClass.getName == "scala.concurrent.forkjoin.ThreadLocalRandom") {
          valueField.set(entry, null)
          true
        } else {
          false
        }
      } else {
        false
      }
    })

  }

}
