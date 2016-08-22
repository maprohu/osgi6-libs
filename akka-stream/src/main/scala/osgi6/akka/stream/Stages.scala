package osgi6.akka.stream

import akka.stream.hack.HackSource
import akka.stream.javadsl.MergePreferred
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream.{SourceShape, _}
import maprohu.scalaext.common.Stateful

import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

/**
  * Created by martonpapp on 29/06/16.
  */
object Stages {

  private object TerminationWatcher extends GraphStageWithMaterializedValue[FlowShape[Any, Any], Future[Unit]] {
    val in = Inlet[Any]("terminationWatcher.in")
    val out = Outlet[Any]("terminationWatcher.out")
    override val shape = FlowShape(in, out)
    override def initialAttributes: Attributes = Attributes.name("terminationWatcher")

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Unit]) = {
      val finishPromise = Promise[Unit]()

      (new GraphStageLogic(shape) {
        setHandler(in, new InHandler {
          override def onPush(): Unit = push(out, grab(in))

          override def onUpstreamFinish(): Unit = {
            finishPromise.success(Unit)
            completeStage()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            finishPromise.failure(ex)
            failStage(ex)
          }
        })
        setHandler(out, new OutHandler {
          override def onPull(): Unit = pull(in)
          override def onDownstreamFinish(): Unit = {
            finishPromise.success(Unit)
            completeStage()
          }
        })
      }, finishPromise.future)
    }

    override def toString = "TerminationWatcher"
  }

  def terminationWatcher[T]: GraphStageWithMaterializedValue[FlowShape[T, T], Future[Unit]] =
    TerminationWatcher.asInstanceOf[GraphStageWithMaterializedValue[FlowShape[T, T], Future[Unit]]]

  case class MapMat[In, Out, Mat](factory: () => Mat)(map: (Mat, In) => Out) extends GraphStageWithMaterializedValue[FlowShape[In, Out], Mat] {
    val in = Inlet[In]("mapmat.in")
    val out = Outlet[Out]("mapmat.out")
    override val shape = FlowShape(in, out)
    override def initialAttributes: Attributes = Attributes.name("mapmat")

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Mat) = {
      val mat = factory()


      (new GraphStageLogic(shape) {
        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            push(out, map(mat, grab(in)))
          }
        })
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })
      }, mat)
    }

    override def toString = "MapMat"
  }

  protected def mergePreferredGraph[T, M](that: Graph[SourceShape[T], M]): Graph[FlowShape[T, T], M] =
    GraphDSL.create(that) { implicit b ⇒
      import GraphDSL.Implicits._

      r ⇒
        val merge = b.add(MergePreferred.create[T](1))
        r ~> merge.preferred
        FlowShape(merge.in(0), merge.out)
    }

  protected def mergeEagerGraph[T, M](that: Graph[SourceShape[T], M]): Graph[FlowShape[T, T], M] =
    GraphDSL.create(that) { implicit b ⇒
      import GraphDSL.Implicits._

      r ⇒
        val merge = b.add(Merge[T](2, true))
        r ~> merge.in(1)
        FlowShape(merge.in(0), merge.out)
    }


  def stopper[T] : Flow[T, T, () => Unit] = {
    Flow[T]
      .map(Some(_))
      .viaMat(
        mergePreferredGraph(
          Source.maybe[Option[T]]
            .mapMaterializedValue({ promise =>
              () => {
                promise.trySuccess(Some(None))
                ()
              }
            })
        )
      )(Keep.right)
      .takeWhile(_.isDefined)
      .map(_.get)

//      .viaMat(mergeEagerGraph(
//        Source.maybe[T]
//          .mapMaterializedValue(promise => () => {
//            promise.trySuccess(None)
//            ()
//          })
//      ))(Keep.right)
  }

  def switcher[T, M1, M2](
    first: Sink[T, M1],
    second: Sink[T, M2]
  ) : Sink[T, (() => Unit, M1, M2)] = {
    Flow[T]
      .viaMat(
        MapMat(() => Stateful(true))((st, e) => (e, st.extract))
      )((_, sw) => () => sw.update(_ => Some(false)))
      .alsoToMat(
        Flow[(T, Boolean)]
          .takeWhile(_._2)
          .map(_._1)
          .toMat(first)(Keep.right)
      )(Keep.both)
      .toMat(
        Flow[(T, Boolean)]
          .dropWhile(_._2)
          .buffer(1, OverflowStrategy.backpressure)
          .map(_._1)
          .toMat(second)(Keep.right)
      )(Keep.both)
      .mapMaterializedValue({ case ((sw, m1), m2) => (sw, m1, m2) })

  }



}
