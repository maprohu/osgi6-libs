package osgi6.common

import java.io.{InputStream, OutputStream, PrintWriter, StringWriter}
import java.net.URL
import java.util.UUID

import org.osgi.framework.{Bundle, BundleContext}
import org.osgi.framework.launch.Framework
import org.osgi.framework.startlevel.BundleStartLevel
import org.osgi.framework.wiring.FrameworkWiring

import scala.collection.JavaConversions._

/**
  * Created by pappmar on 05/07/2016.
  */
object OsgiTools {


  def deploy(fw: Framework, bundles: URL*) : Unit = {
    val ctx = fw.getBundleContext
    deployBundle(ctx, bundles:_*)
  }

  def deployBundle(ctx: BundleContext, bundles: URL*) : Unit = {

    val installed =
      bundles
        .map({ url =>
          ctx.installBundle(url.toExternalForm, url.openStream())
        })

    installed.foreach({ bundle =>
      bundle.adapt(classOf[BundleStartLevel]).setStartLevel(1)
      bundle.start()
    })
  }

  def deployFragment(ctx: BundleContext, stream: InputStream) : Bundle = {
    val bundle = ctx.installBundle(UUID.randomUUID().toString, stream)
    bundle
  }

  def installBundle0(ctx: BundleContext, stream: InputStream) : Bundle = {
    ctx.installBundle(UUID.randomUUID().toString, stream)
  }

  def deployBundle0(ctx: BundleContext, stream: InputStream) : Bundle = {
    val bundle = ctx.installBundle(UUID.randomUUID().toString, stream)
    bundle.adapt(classOf[BundleStartLevel]).setStartLevel(1)
    bundle.start()
    bundle
  }

  def deployBundle(ctx: BundleContext, stream: InputStream) : String = {
    try {
      val bundle = deployBundle0(ctx, stream)
      bundle.getBundleId.toString
    } catch {
      case ex: Throwable =>
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        ex.printStackTrace(pw)
        pw.close()
        sw.close()
        sw.toString
    }
  }

  def execBundle(ctx: BundleContext, stream: InputStream, out: OutputStream) : Unit = {
    try {
      val bundle = installBundle0(ctx, stream)
      try {
        bundle.start()
        val implClass = bundle.loadClass(OsgiAdmin.AdminClassName)
        val adminMethod = implClass.getMethod(OsgiAdmin.AdminMethodName, OsgiAdmin.AdminMethodParameters:_*)
        val instance = implClass.newInstance()
        adminMethod.invoke(instance, bundle.getBundleContext, out)
      } finally {
        undeployBundle(ctx, bundle.getBundleId)
      }
    } catch {
      case ex: Throwable =>
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        ex.printStackTrace(pw)
        pw.close()
        sw.close()
        out.write(sw.toString.getBytes)
    }
  }

  //    def undeployBundle(fw: Framework, id: Long) : String = {
  def undeployBundle(ctx : BundleContext, id: Long) : String = {
    val bnd = ctx.getBundle(id)
    bnd.uninstall()
//    fw.adapt(classOf[FrameworkWiring]).refreshBundles(
//      Seq(bnd)
//    )
    s"bundle ${id} uninstalled"
  }

  def refresh(fw: Framework) = {
    fw.adapt(classOf[FrameworkWiring]).refreshBundles(
      fw.getBundleContext.getBundles.toSeq
    )
  }

  def refresh(bundle: Bundle) = {
    bundle.getBundleContext.getBundle(0).adapt(classOf[FrameworkWiring]).refreshBundles(
      Seq( bundle )
    )
  }
}
