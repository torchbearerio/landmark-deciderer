package io.torchbearer.landmarkdeciderer.NLP

import io.torchbearer.landmarkdeciderer.NLP.Word2Vec._

/**
  * Created by fredricvollmer on 1/25/18.
  */
object Word2VecSimilarity {
  val model = new Word2Vec()
  println("Loading word vectors...")
  model.load("vectors.bin")
  println("Loaded.")

  def sequenceSimilarity(words1: List[String], words2: List[String]): Double = {
    // We determine the similarity of two sequences of word vectors by summing all vectors in each sequence,
    // then finding the similarity between those vectors

    // If our model doesn't contain the vector for a word, we ignore it
    val filteredWords1 = words1.filter(w => model.contains(w))
    val filteredWords2 = words2.filter(w => model.contains(w))

    val v1 = model.sumVector(filteredWords1)
    val v2 = model.sumVector(filteredWords2)

    model.cosine(v1, v2)

    /*
    val scores = for (w1 <- words1; w2 <- words2)
      yield if (model.contains(w1) && model.contains(w2))
        model.cosine(w1, w2)
      else
        0

    scores.sum / scores.size
    */
  }
}
