package traffic

import akka.stream._
import akka.stream.scaladsl.FlexiMerge.{MergeLogic, Read}
import akka.stream.scaladsl.FlexiRoute.{DemandFromAll, RouteLogic}
import akka.stream.scaladsl._

import scala.util.Random

class OnRampShape(_init: FanInShape.Init[Car] = FanInShape.Name("OnRamp")) extends FanInShape[Car](_init) {
  override protected def construct(init: FanInShape.Init[Car]): FanInShape[Car] = new OnRampShape(init)

  val entrance = newInlet[Car]("Entrance")
  val freeway = newInlet[Car]("Freeway")
}

class OnRamp() extends FlexiMerge[Car, OnRampShape](new OnRampShape(), Attributes.name("OnRamp")) {
  val threshold = 25

  override def createMergeLogic(s: OnRampShape): MergeLogic[Car] = new MergeLogic[Car] {
    def consumeAll(currentInlet: Inlet[Car], nextInlet: Inlet[Car], threshold: Int): State[Car] = State[Car](Read(currentInlet)) {
      case (ctx, input, car) =>
        ctx.emit(car)
        if (Random.nextInt(100) > threshold)
          consumeAll(nextInlet, currentInlet, 100 - threshold)
        else
          SameState
    }

    override def initialCompletionHandling: CompletionHandling = CompletionHandling(
      onUpstreamFinish = (ctx, input) => input match {
        case s.entrance => consumeAll(s.freeway, s.freeway, 0)
        case s.freeway => consumeAll(s.entrance, s.entrance, 0)
        case _ => SameState
      },
      onUpstreamFailure = (ctx, input, cause) => {
        ctx.fail(cause)
        SameState
      }
    )

    override def initialState: State[_] = consumeAll(s.freeway, s.entrance, 25)
  }

}

class OffRampShape(_init: FanOutShape.Init[Car] = FanOutShape.Name[Car]("OffRamp")) extends FanOutShape[Car](_init) {
  override protected def construct(init: FanOutShape.Init[Car]): FanOutShape[Car] = new OffRampShape(init)

  val freeway = newOutlet[Car]("Freeway")
  val exit = newOutlet[Car]("Exit")
  inlets
}

class OffRamp() extends FlexiRoute[Car, OffRampShape](new OffRampShape(), Attributes.name("OffRamp")) {
  val threshold = 25

  override def createRouteLogic(s: OffRampShape): RouteLogic[Car] = new RouteLogic[Car] {
    override def initialState: State[_] = State(DemandFromAll(s.freeway, s.exit)) {
      case (ctx, output, car) if Random.nextInt(100) > threshold =>
        ctx.emit(s.freeway)(car)
        SameState
      case (ctx, output, car) =>
        ctx.emit(s.exit)(car)
        SameState
    }
  }
}

trait TrafficFlow {
  def myFreeway = FlowGraph.closed() { implicit builder =>
    import FlowGraph.Implicits._

    def count(id: String) = Sink.fold[Int, Car](0) {
      (counter, car) =>
        println(s"[$id, ${counter + 1}] $car")
        counter + 1
    }

    val partsUnknown = Source(1 to 10).map(_ => Car("parts unknown"))
    val beyond = count("beyond")
    val country = Source(1 to 10).map(_ => Car("country"))
    val countryOnRamp = builder.add(new OnRamp)
    val suburb = Source(1 to 10).map(_ => Car("suburb"))
    val suburbOnRamp = builder.add(new OnRamp)
    val city = Source(1 to 10).map(_ => Car("city"))
    val cityOnRamp = builder.add(new OnRamp)
    val stadium = count("stadium")
    val stadiumOffRamp = builder.add(new OffRamp)
    val theater = count("theater")
    val theaterOffRamp = builder.add(new OffRamp)
    val mall = count("mall")
    val mallOffRamp = builder.add(new OffRamp)

    partsUnknown ~> countryOnRamp.freeway
    country ~> countryOnRamp.entrance
    countryOnRamp.out ~> suburbOnRamp.freeway
    suburb ~> suburbOnRamp.entrance
    suburbOnRamp.out ~> cityOnRamp.freeway
    city ~> cityOnRamp.entrance
    cityOnRamp.out ~> stadiumOffRamp.in
    stadiumOffRamp.exit ~> stadium
    stadiumOffRamp.freeway ~> theaterOffRamp.in
    theaterOffRamp.exit ~> theater
    theaterOffRamp.freeway ~> mallOffRamp.in
    mallOffRamp.exit ~> mall
    mallOffRamp.freeway ~> beyond
  }
}
