package io.torchbearer.landmarkdeciderer

/**
  * Created by fredricvollmer on 10/18/17.
  */
object Constants {
  final val POS_TAGS: Map[String, String] = Map(
      "ADJ" -> "JJ",
      "COMP_ADJ" -> "JJR",
      "SUP_ADJ" -> "JJS",
      "NOUN" -> "NN",
      "PLURAL_NOUN" -> "NNS"
  )
}
