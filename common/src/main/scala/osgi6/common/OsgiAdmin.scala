package osgi6.common

import java.io.{OutputStream, PrintWriter}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.osgi.framework.BundleContext

import scala.util.control.NonFatal


/**
  * Created by pappmar on 17/08/2016.
  */
object OsgiAdmin {

  val AdminClassName = "osgi6.admin.impl.AdminImpl"
  val AdminMethodName = "execute"
  val AdminMethodParameters = Seq(classOf[BundleContext], classOf[OutputStream])

  def processAdminRequestStream(
    req: HttpServletRequest,
    resp: HttpServletResponse,
    ct: String = "text/plain"
  )(
    fn: OutputStream => Unit
  ) = {
    resp.setContentType(ct)
    HttpTools.preResponse(req, resp)
    val os = resp.getOutputStream
    try {
      try {
        fn(os)
      } catch {
        case ex : Throwable =>
          val pw = new PrintWriter(os)
          ex.printStackTrace(pw)
          pw.close()
      }
    } finally {
      os.close()
    }
  }

  def dispatch(
    ctx: BundleContext,
    req: HttpServletRequest,
    resp: HttpServletResponse
  ) = {
    processAdminRequestStream(req, resp, "text/plain") { os =>
      OsgiTools.execBundle(
        ctx,
        req.getInputStream,
        os
      )
    }
  }
}
