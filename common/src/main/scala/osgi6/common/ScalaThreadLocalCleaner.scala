package osgi6.common

import java.lang.reflect.Field

import scala.concurrent.forkjoin.ThreadLocalRandom

/**
  * Created by pappmar on 26/08/2016.
  */
object ScalaThreadLocalCleaner {

  case class CleanResult(
    cleaned: Seq[String],
    suspicious: Seq[String],
    retained: Seq[String]
  )

  val leakingClassNames = Set(
    "scala.concurrent.forkjoin.ForkJoinPool$Submitter",
    classOf[ThreadLocalRandom].getName
  )

  def shouldClean(value: Any) : Boolean = {
    leakingClassNames.contains(value.getClass.getName)
  }

  def cleanScalaThreadLocals : CleanResult = {
    val thread: Thread = Thread.currentThread

    val threadLocalsField: Field = classOf[Thread].getDeclaredField("threadLocals")
    threadLocalsField.setAccessible(true)

    val threadLocalMapKlazz: Class[_] = Class.forName("java.lang.ThreadLocal$ThreadLocalMap")
    val tableField: Field = threadLocalMapKlazz.getDeclaredField("table")
    tableField.setAccessible(true)

    val table: Any = tableField.get(threadLocalsField.get(thread))
    val threadLocalCount: Int = java.lang.reflect.Array.getLength(table)

    (0 until threadLocalCount).foldLeft(CleanResult(Seq(),Seq(), Seq()))({ (acc, index) =>
      val entry: Any = java.lang.reflect.Array.get(table, index)
      if (entry != null) {
        val valueField: Field = entry.getClass.getDeclaredField("value")
        valueField.setAccessible(true)
        val value: Any = valueField.get(entry)
        if (value != null) {
          val valueClassName = value.getClass.getName

          if (shouldClean(value)) {
            valueField.set(entry, null)
            acc.copy(cleaned = acc.cleaned :+ valueClassName)
          } else if (valueClassName.startsWith("scala.")) {
            acc.copy(suspicious = acc.suspicious :+ valueClassName)
          } else {
            acc.copy(retained = acc.retained :+ valueClassName)
          }
        } else {
          acc
        }
      } else {
        acc
      }
    })

  }

}
