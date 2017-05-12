package io.torchbearer.landmarkdeciderer

import java.io.File

import io.torchbearer.ServiceCore.Orchestration.Task
import io.torchbearer.ServiceCore.DataModel.ExecutionPoint
import io.torchbearer.ServiceCore.AWSServices.S3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import io.torchbearer.ServiceCore.Constants
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors

/**
  * Created by fredricvollmer on 4/13/17.
  */
class LandmarkDecidererTask(epId: Int, hitId: Int, distance: Int, taskToken: String)

  extends Task(epId = epId, hitId = hitId, taskToken = taskToken) {

  override def run(): Unit = {



    try {
      this.sendSuccess()
    }
    catch {
      case _: Throwable => sendFailure("Streetview Loader Error", "Unable to save streetview image")
    }
  }
}
