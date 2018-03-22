package io.torchbearer.landmarkdeciderer

import io.torchbearer.ServiceCore.AWSServices.SFN.getTaskForActivityArn
import io.torchbearer.ServiceCore.{Constants => CoreConstants, TorchbearerDB}
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object LandmarkDeciderer extends App {
  implicit val formats = DefaultFormats

  TorchbearerDB.init()

  println("Welcome to landmark-deciderer")

  //val decideTask = new LandmarkDecidererTask(432, 884, "AAAAKgAAAAIAAAAAAAAAAfRKslUEeabEMjQS3N9JQz95IDJgNbOSbhglStKrCE4qoR++Z4wHY1RNPELenVWiZNYNGyF2uQa86YL9ELdlm/MQoEcbMomzEnnPeWITXmNWzMIY1OFH71dHMsYGsGrURRMVVz8WVhPub/fKNzkejgwrvAm3I+rK3p9QIPwl244JapHCGJVPD8hTcK/k7YW3ns62ajneu98zJLKUU2MygOeLcnIj4NGbp7O2iq85v9bfC6gbH8BdCKFwbWXHMBy8YHItXiJPvxYP3qNSN3r+kP+OjwQy3xi3l309eeXufO4sWK7fAaeUuFqXWH1YivKHSzdvok/rS2RX7wFgejc9ETHFgutpDtofMEL3zPZQolSv6q2JkJzpA9W8U+0aqIqsz/13IkNMDEzlAN9zPY0yCaWJJGIGtLbpNziFigrspR21F/H99+PVSEnbAqIOvWduq561Pg6S5imD7J811H4E4O98M8aSdL9OKEhXwY9g5bTYWcqlHmnKTVj0iReXEylETHMT38gi/kG0PLkqWXLZn1T5df8ZUDK6/cqEaUoJJqKPeEijjroUd2Ej+qsFzwp/5w==")
  //decideTask.run()

  while (true) {
    val task = getTaskForActivityArn(CoreConstants.ActivityARNs("LANDMARK_DECIDERER"))

    // If no tasks were returned, exit
    if (task.getTaskToken != null) {

      val input = parse(task.getInput)
      val epId = (input \ "epId").extract[Int]
      val hitId = (input \ "hitId").extract[Int]
      val taskToken = task.getTaskToken

      val decideTask = new LandmarkDecidererTask(epId, hitId, taskToken)

      Future {
        blocking {
          decideTask.run()
        }
      }
    }
  }
}
