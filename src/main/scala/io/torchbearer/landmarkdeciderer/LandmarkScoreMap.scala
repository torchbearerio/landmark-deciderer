package io.torchbearer.landmarkdeciderer

import io.torchbearer.ServiceCore.DataModel.Landmark

/**
  * Created by fredricvollmer on 5/19/17.
  */
class LandmarkScoreMap(landmarks: Iterable[Landmark]) {
  private var scoreMap = landmarks.map(lm => {
    (lm, (landmarks.toSet - lm).map(lm2 => (lm2, 0D)).toMap)
  }).toMap

  def getSimilarityScoreForLandmark(landmark: Landmark): Double = {
    if (!scoreMap.contains(landmark)) {
      return 0
    }
    scoreMap(landmark).values.sum
  }

  def setSimilarityScoreForLandmarks(lmA: Landmark, lmB: Landmark, score: Double): Unit = {
    val newInnerMapA = scoreMap(lmA).updated(lmB, score)
    val newInnerMapB = scoreMap(lmB).updated(lmA, score)
    scoreMap = scoreMap.updated(lmA, newInnerMapA)
    scoreMap = scoreMap.updated(lmB, newInnerMapB)
  }
}
