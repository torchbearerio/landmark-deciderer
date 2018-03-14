package io.torchbearer.landmarkdeciderer

import java.io.File

import io.torchbearer.ServiceCore.{Constants => CoreConstants}
import io.torchbearer.ServiceCore.DataModel.{Hit, Landmark, LandmarkStatus}
import io.torchbearer.ServiceCore.Orchestration.Task
import io.torchbearer.landmarkdeciderer.NLP.Word2VecSimilarity
import io.torchbearer.landmarkdeciderer.NLP.Filters._

/**
  * Created by fredricvollmer on 4/13/17.
  */
class LandmarkDecidererTask(epId: Int, hitId: Int, taskToken: String)
  extends Task(epId = epId, hitId = hitId, taskToken = taskToken) {

  override def run(): Unit = {
    println(s"Deciding landmark for ep $epId, hit $hitId")

    try {
      // Get hit
      val hit = Hit.getHit(hitId) getOrElse {
        throw new Exception("Error: Unable to load Hit")
      }

      // Get all Landmarks for this Hit
      val landmarks = Landmark.getLandmarksForHit(hitId, Some(Seq(LandmarkStatus.VERIFIED)))

      if (landmarks.isEmpty) {
        println(s"No landmarks found for hit $hitId")
        // Send success
        sendSuccess()
        return
      }

      // How we choose Landmark will vary with pipeline:
      val bestLandmark = hit.pipeline match {
        // Maximize visual saliency, semantic saliency and structural saliency scores
        case CoreConstants.PIPELINES_CV_CV =>
          calculateAndUpdateStructuralSaliencyScores(landmarks)
          normalizeSaliencyScores(landmarks)
          landmarks.maxBy(lm => lm.structuralSaliencyScore + lm.visualSaliencyScore + lm.semanticSaliencyScore)

        // Maximize visual and structural saliency--we don't have semantic saliency yet.
        case CoreConstants.PIPELINES_CV_HUMAN =>
          calculateAndUpdateStructuralSaliencyScores(landmarks)
          normalizeSaliencyScores(landmarks)
          landmarks.maxBy(lm => lm.structuralSaliencyScore + lm.visualSaliencyScore)

        // If saliency detection was performed by human, we prioritize that score.
        // Structural saliency is used only as tie-breaker. Semantic saliency does not exist.
        case CoreConstants.PIPELINES_HUMAN_CV | CoreConstants.PIPELINES_HUMAN_HUMAN =>
          val maxScoreLandmark = landmarks.maxBy(_.visualSaliencyScore)
          val bestLandmarks = landmarks.filter(_.visualSaliencyScore == maxScoreLandmark.visualSaliencyScore)

          // If there's a tie, resort to uniqueness as tiebreaker (structural saliency)
          if (bestLandmarks.length > 1) {
            getMostUniqueLandmark(bestLandmarks)
          }

          // Otherwise, use the only landmark in the seq
          else {
            bestLandmarks.head
          }

        case _ => throw new Exception("Unknown pipeline")
      }

      // Update the selected landmark's computedDescription
      // If there is only one description (i.e., human description) use that
      /*
      var bestWord = if (bestLandmark.description.get.size == 1) {
        bestLandmark.description.get.keys.head
      }
      else {
        // Otherwise (i.e. CV description), select description words within 10% of max p
        val maxP = bestLandmark.description.get.maxBy(t => t._2)._2
        val selectedWords = bestLandmark.description.get.filter(t => t._2 / maxP >= 0.9)

        // Out of selected words, find most "specific" based on WordNet depth
        selectedWords.maxBy(t => DepthFinder.getWordnetDepth(t._1))._1
      }
      */

      // If landmark color is defined, prepend to bestWord
      val colors = bestLandmark.color.getOrElse(Nil)
      val Last = colors.length - 1
      val SecondToLast = colors.length - 2
      val colorString = colors.zipWithIndex.foldLeft(new StringBuilder())((sb, t) => {
        t._2 match {
          case Last => sb ++= s"${t._1}"
          case SecondToLast => sb ++= s"${t._1} and "
          case _ => sb ++= s"${t._1}, "
        }
      }).toString

      val computedDesc = s"$colorString ${bestLandmark.description getOrElse "thing"}"

      // Update the selected landmark's computedDescription in DB
      bestLandmark.updateComputedDescription(computedDesc)

      // Update this Hit's selected landmark
      hit.updateSelectedLandmark(bestLandmark.landmarkId)

      // Send success
      sendSuccess()

      println(s"Done deciding landmark for ep $epId, hit $hitId")
    }
    catch {
      case e: Throwable => {
        e.printStackTrace()
        sendFailure("Landmark Deciderer error", e.getMessage)
      }
    }
  }

  def getMostUniqueLandmark(landmarks: List[Landmark]): Landmark = {
    val scoreMap = getCrossSimilarity(landmarks)
    landmarks.minBy(lm => scoreMap.getSimilarityScoreForLandmark(lm))
  }

  def calculateAndUpdateStructuralSaliencyScores(landmarks: List[Landmark]) = {
    // Retrieve cross-similarity
    val scoresMap = getCrossSimilarity(landmarks)

    // Update Landmark structural saliency scores
    landmarks.foreach(lm => {
      val score = scoresMap.getSimilarityScoreForLandmark(lm)
      lm.updateStructuralSaliencyScore(score)
    })
  }

  def getCrossSimilarity(landmarks: List[Landmark]): LandmarkScoreMap = {
    val landmarksPairs = landmarks.combinations(2)
    val scoreMap = new LandmarkScoreMap(landmarks)

    landmarksPairs.foreach { case List(lmA, lmB) => {
      val aDesc = lmA.description getOrElse ""
      val bDesc = lmB.description getOrElse ""

      if (aDesc == "" || bDesc == "") {
        scoreMap.setSimilarityScoreForLandmarks(lmA, lmB, 0)

      } else {
        var aDescSeq = aDesc
          .stripPunctuation
          .trim
          .split("\\s+")
          .filter(!_.isStopWord)
          .toList
        var bDescSeq = bDesc
          .stripPunctuation
          .trim
          .split("\\s+")
          .filter(!_.isStopWord)
          .toList

        // If either landmark has colors associated with it, add these into the map of descriptions, with adjective pos
        aDescSeq ++= lmA.color.getOrElse(Nil)
        bDescSeq ++= lmB.color.getOrElse(Nil)

        val score = Word2VecSimilarity.sequenceSimilarity(aDescSeq, bDescSeq)
        scoreMap.setSimilarityScoreForLandmarks(lmA, lmB, score)
      }
    }
    }
    scoreMap
  }

  /**
    * Maps visual and strucutral saliency scores to [0,1] range
    * NOTE: Modifies Landmark objects, but does NOT update database.
    *
    * @param landmarks {List[Landamrk]}
    */
  def normalizeSaliencyScores(landmarks: List[Landmark]) = {
    val minVisual = landmarks.minBy(_.visualSaliencyScore).visualSaliencyScore
    val maxVisual = landmarks.maxBy(_.visualSaliencyScore).visualSaliencyScore
    val minStructural = landmarks.minBy(_.structuralSaliencyScore).structuralSaliencyScore
    val maxStrucutral = landmarks.maxBy(_.structuralSaliencyScore).structuralSaliencyScore
    val minSemantic = landmarks.minBy(_.semanticSaliencyScore).semanticSaliencyScore
    val maxSemantic = landmarks.maxBy(_.semanticSaliencyScore).semanticSaliencyScore

    landmarks.foreach(lm => {
      if (maxStrucutral - minStructural > 0)
        lm.structuralSaliencyScore = (lm.structuralSaliencyScore - minStructural) / (maxStrucutral - minStructural)

      if (maxVisual - minVisual > 0)
        lm.visualSaliencyScore = (lm.visualSaliencyScore - minVisual) / (maxVisual - minVisual)

      if (maxSemantic - minSemantic > 0)
        lm.semanticSaliencyScore = (lm.semanticSaliencyScore - minSemantic) / (maxSemantic - minSemantic)
    })
  }

  def expandMultiWordKeys(desc: Map[String, Double]): Map[String, Double] = {
    // Find keys that are multi-word
    val multiWordKeysMap = desc.filter(t => t._1.contains(" "))

    // Multi-word keys need to be pos tagged


    // Expand them into multiple keys
    val expandedKeyMap = multiWordKeysMap.flatMap(t => {
      // Make new keys out of each space-separated word
      val keys = t._1.split(" ")

      // Give each new key the same probability as original, multi-word key
      keys.map(k => (k, t._2))
    })

    var newDescMap = desc -- multiWordKeysMap.keys
    newDescMap = expandedKeyMap.foldLeft(newDescMap)((descMap, t) => descMap.updated(t._1, t._2))

    newDescMap
  }
}