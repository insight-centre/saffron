package org.insightcentre.nlp.saffron.atr4s

import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;

import ru.ispras.atr.features.FeatureConfig
import ru.ispras.atr.features.refcorpus.{Weirdness, ReferenceCorpusConfig}
import ru.ispras.atr.rank.{OneFeatureTCWeighterConfig, VotingTCWeighterConfig}
import ru.ispras.atr.candidates.TCCConfig
import ru.ispras.atr.preprocess._
import ru.ispras.atr.ATRConfig
import scala.collection.JavaConversions._

class TopicExtraction(config : Configuration) {
  val nlpPreprocessor = EmoryNLPPreprocessorConfig().build()
  val candidatesCollector = TCCConfig(config.ngramMin to config.ngramMax, config.minTermFreq).build()
  val candidatesWeighter = config.method match {
    case "one" =>
      OneFeatureTCWeighterConfig(mkFeats(config)(0)).build()
    case "voting" =>
      VotingTCWeighterConfig(mkFeats(config)).build()
    case _ =>
      throw new UnsupportedOperationException("Unknown method: " + config.method)
  }
  val threshold = config.threshold

  def mkFeats(config : Configuration) : Seq[FeatureConfig] = {
    config.features.map({ 
      case "weirdness" => Weirdness(ReferenceCorpusConfig(config.corpus))
    })
  }

  def extractTopics(searcher : DocumentSearcher) : (List[DocumentTopic],Set[Topic]) = {
    val docs = searcher.allDocuments()
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
    while({ j = string.indexOf(sub_string,j) ; j} >= 0) {
      i += 1
      j += 1
    }
    i
  }
}
