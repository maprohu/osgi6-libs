package osgi6.multi.api

import java.io.File
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
  * Created by martonpapp on 04/07/16.
  */
object MultiApiTrait {

  trait Callback {
    def handled(result: Boolean): Unit
  }

  trait Handler {
    def dispatch(request: HttpServletRequest, response: HttpServletResponse, callback: Callback): Unit
  }

  trait Registration {
    def remove: Unit
  }

  trait Registry {
    def register(handler: Handler): Registration

    def iterate: java.util.Enumeration[Handler]
  }

}

trait MultiApiTrait {
  type Registry = MultiApiTrait.Registry
  type Callback = MultiApiTrait.Callback
  type Handler = MultiApiTrait.Handler
  type Registration = MultiApiTrait.Registration

  def registry : Registry

}

object ContextApiTrait {

  trait Handler {
    def dispatch(ctx: Context): Unit
  }

  trait Registration {
    def remove: Unit
  }

  trait Registry {
    def listen(handler: Handler): Registration

    def set(ctx: Context): Unit
  }

}

trait ContextApiTrait {
  type Handler = ContextApiTrait.Handler
  type Registration = ContextApiTrait.Registration
  type Registry = ContextApiTrait.Registry

  val registry : Registry

}

trait Context {
  def name: String
  def data: File
  def log: File
  def debug: Boolean
  def stdout: Boolean
  def rootPath : String
  def servletConfig: ServletConfig
}

