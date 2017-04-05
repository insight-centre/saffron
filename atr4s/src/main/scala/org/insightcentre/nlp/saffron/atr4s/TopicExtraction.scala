package org.insightcentre.nlp.saffron.atr4s

import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.Topic.MorphologicalVariation;
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
    val topics = {
      val dataset = nlpPreprocessor.preprocess(docs.map(doc =>
          (doc.id, doc.getContents())).toSeq)
      val candidates = candidatesCollector.collect(dataset)
      val sortedTerms = candidatesWeighter.weightAndSort(candidates, dataset)
      val filteredTerms = sortedTerms.filter(_._2 > threshold)
      val can2lemmas = candidates.map(tc => tc.canonicalRepr -> tc.lemmas).toMap
      filteredTerms.map({ case (string, score) =>
        val lemmas = can2lemmas(string)
        val lemma1 = lemmas(0).toLowerCase
        new Topic(lemma1, -1, -1, score, lemmas.map(l => new MorphologicalVariation(l)))
      })
    }
    //val topics = terms.map({ case (string, score) =>
    //  new Topic(string, -1, -1, score, null)
    //})
    val documentTopics = docs.flatMap({ doc =>
      topics.flatMap({ topic =>
        val f = searcher.numberOfOccurrences(topic.topicString)
        if(f > 0) {
          Some(new DocumentTopic(doc.id, topic.topicString, f.toInt, null, null))
        } else {
          None
        }
      })
    })
    (documentTopics.toList, topics.toSet)
  }
}
