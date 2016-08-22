package osgi6.common

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
  * Created by pappmar on 19/08/2016.
  */
object HttpTools {

  def preResponse(req: HttpServletRequest, resp: HttpServletResponse) = {
//    resp.setHeader("Connection", "close")
  }


}
