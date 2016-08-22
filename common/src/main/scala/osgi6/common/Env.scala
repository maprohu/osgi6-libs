package osgi6.common

/**
  * Created by pappmar on 04/07/2016.
  */
object Env {

  def hostname : Option[String] =
    sys.env.get("HOSTNAME")

  def choose[T](
    dev : => T,
    test : => T,
    preprod : => T,
    prod : => T
  )(prefix: String = "wls") : T = {
    hostname
      .map(_.toLowerCase)
      .collect({
        case hn if hn.startsWith(s"t${prefix}") => test
        case hn if hn.startsWith(s"q${prefix}") => preprod
        case hn if hn.startsWith(s"p${prefix}") => prod
      })
      .getOrElse(dev)
  }

}
