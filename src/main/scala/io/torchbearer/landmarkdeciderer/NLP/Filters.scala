package io.torchbearer.landmarkdeciderer.NLP

import edu.cmu.lti.ws4j.util.StopWordRemover

object Filters {
  private lazy val stopWordRemover = StopWordRemover.getInstance()

  implicit class StringExtensions(s: String) {
    def isStopWord: Boolean = {
      stopWordRemover.removeStopWords(Array(s)).isEmpty
    }

    def stripPunctuation: String = {
      s.replaceAll("""[\p{Punct}\n\r]""", "")
    }
  }

}