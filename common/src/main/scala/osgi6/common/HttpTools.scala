package osgi6.common

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
  * Created by pappmar on 19/08/2016.
  */
object HttpTools {

  def preResponse(req: HttpServletRequest, resp: HttpServletResponse) = {
//    resp.setHeader("Connection", "close")
  }


  def cleanScalaThreadLocals(path: String, req: HttpServletRequest, res: HttpServletResponse) : Boolean = {
    if (req.getRequestURI == path) {
      res.setContentType("text/plain")
      res.setStatus(HttpServletResponse.SC_OK)
      try {
        val out = res.getWriter

        out.println(Thread.currentThread().getName)
        new Exception().printStackTrace(out)
        out.println(ScalaThreadLocalCleaner.cleanScalaThreadLocals)
      } catch {
        case ex : Throwable =>
          res.getWriter.println(s"Error cleaning: ${ex}")
      }
      true
    } else {
      false
    }
  }

}
