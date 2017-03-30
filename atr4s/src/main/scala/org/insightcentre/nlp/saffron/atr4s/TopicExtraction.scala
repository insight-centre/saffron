package org.insightcentre.nlp.saffron.atr4s

import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

import ru.ispras.atr.features.refcorpus.Weirdness
import ru.ispras.atr.rank.OneFeatureTCWeighterConfig
// Be careful with this class one of the Cs is actually a cyrillic letter
import ru.ispras.atr.candidates.TCCConfig
import ru.ispras.atr.preprocess._
import ru.ispras.atr.ATRConfig

class TopicExtraction(config : Configuration) {
  val nlpPreprocessor = EmoryNLPPreprocessorConfig().build()
  val candidatesCollector = TCCConfig().build()
  val candidatesWeighter = OneFeatureTCWeighterConfig(Weirdness()).build()
  val threshold = config.threshold

  def extractTopics(docs : Iterable[Document]) : (List[DocumentTopic],Set[Topic]) = {
    val terms = {
      val dataset = nlpPreprocessor.preprocess(docs.map(doc =>
          (doc.id, doc.getContents())).toSeq)
      val candidates = candidatesCollector.collect(dataset)
      val sortedTerms = candidatesWeighter.weightAndSort(candidates, dataset)
      sortedTerms.filter(_._2 > threshold)

    }
    val topics = terms.map({ case (string, score) =>
      new Topic(string, -1, -1, score, null)
    })
    val documentTopics = docs.flatMap({ doc =>
      topics.flatMap({ topic =>
        val f = freq(doc.getContents(), topic.topicString)
        if(f > 0) {
          Some(new DocumentTopic(doc.id, topic.topicString, f, null, null))
        } else {
          None
        }
      })
    })
    (documentTopics.toList, topics.toSet)
  }

  def freq(string : String, sub_string : String) = {
    var i = 0
    var j = 0
    while({ j = string.indexOf(sub_string,j) ; j} >= 0) i += 1
    i
  }
}
