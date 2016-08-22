package osgi6.scalarx

import rx.Ctx.Owner.Unsafe
import rx.Var

/**
  * Created by pappmar on 06/07/2016.
  */
class ListenableRegistry[Value, Listener, Registration, Unset](
  notify: (Listener, Value) => Unit,
  unregister: (() => Unit) => Registration,
  unset: (() => Unit) => Unset
) {

  var values = List[Option[Value]]()

  val rxVar = Var(Option.empty[Value])

  def listen(handler: Listener) : Registration = {

    import Unsafe.Unsafe
    val obs = rxVar.foreach { value =>
      notify(handler, value.getOrElse(null.asInstanceOf[Value]))
    }

    unregister(() => obs.kill())
  }


  def set(value: Value) : Unset = synchronized {
    val valueOpt = Option(value)
    values = valueOpt +: values
    rxVar() = valueOpt

    unset(() => synchronized {
      values = values diff List(valueOpt)
      rxVar() = values.headOption.flatten
    })
  }

}
