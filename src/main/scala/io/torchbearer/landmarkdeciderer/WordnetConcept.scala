package io.torchbearer.landmarkdeciderer

/**
  * Created by fredricvollmer on 5/22/17.
  */
case class WordnetConcept(var lemma: String,
                          var pos: String = "",
                          var index: Int = 0,
                          var osmURI: String = "") {
}