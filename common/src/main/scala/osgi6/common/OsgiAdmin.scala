package osgi6.common

import java.io.OutputStream

import org.osgi.framework.BundleContext


/**
  * Created by pappmar on 17/08/2016.
  */
object OsgiAdmin {

  val AdminClassName = "osgi6.admin.impl.AdminImpl"
  val AdminMethodName = "execute"
  val AdminMethodParameters = Seq(classOf[BundleContext], classOf[OutputStream])

}
