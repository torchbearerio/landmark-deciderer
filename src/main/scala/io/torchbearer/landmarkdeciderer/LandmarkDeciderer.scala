package io.torchbearer.landmarkdeciderer

import io.torchbearer.ServiceCore.AWSServices.SFN.getTaskForActivityArn
import io.torchbearer.ServiceCore.Constants
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object LandmarkDeciderer extends App {
  implicit val formats = DefaultFormats

  println("Welcome to streetview-loader")

  while (true) {
    val task = getTaskForActivityArn(Constants.ActivityARNs("STREETVIEW_IMAGE_LOAD"))

    // If no tasks were returned, exit
    if (task.getTaskToken != "") {

      val input = parse(task.getInput)
      val epId = (input \ "epId").extract[Int]
      val hitId = (input \ "hitId").extract[Int]
      val taskToken = task.getTaskToken

      val loadTask = new LandmarkDecidererTask(epId, hitId, Constants.DEFAULT_IMAGE_DISTANCE, taskToken)

      Future {
        blocking {
          loadTask.run()
        }
      }
    }
  }
}
