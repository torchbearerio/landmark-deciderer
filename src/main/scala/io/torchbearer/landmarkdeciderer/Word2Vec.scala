package io.torchbearer.landmarkdeciderer

import java.io.File

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer

/**
  * Created by fredricvollmer on 5/12/17.
  */
object Word2Vec {
  private val url = getClass.getResource("glove.6B.50d.txt")
  private val file = new File(url.getPath)
  val gloveVectors = WordVectorSerializer.loadTxtVectors(file)
}
