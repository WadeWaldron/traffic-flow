package traffic

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object TrafficFlowApp extends App with TrafficFlow {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  myFreeway.run()

  Thread.sleep(5000)

  system.shutdown()
  system.awaitTermination()
}
