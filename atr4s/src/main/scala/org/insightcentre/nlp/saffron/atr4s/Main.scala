package org.insightcentre.nlp.saffron.atr4s

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File
import joptsimple.OptionParser
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcherFactory;
import scala.collection.JavaConversions._

object Main {
  def badOptions(p : OptionParser, message : String) {
    System.err.println("Error: "  + message);
    p.printHelpOn(System.err);
    System.exit(-1);
  }

  def main(args : Array[String]) {
    val p = new OptionParser() {
      accepts("c", "The configuration to use").withRequiredArg().ofType(classOf[File])
      accepts("x", "The text corpus to use").withRequiredArg().ofType(classOf[File])
      accepts("t", "Where to write the topics to").withRequiredArg().ofType(classOf[File])
      accepts("o", "Where to write the document-topic mapping to").withRequiredArg().ofType(classOf[File])
    }
    val os = try { 
      p.parse(args:_*)
    } catch {
      case x : Throwable =>
        badOptions(p, x.getMessage())
        return
    }
    val configuration = os.valueOf("c").asInstanceOf[File]
    if(configuration == null || !configuration.exists()) {
      badOptions(p, "Configuration does not exist");
    }
    val corpusFile = os.valueOf("x").asInstanceOf[File]
    if(corpusFile == null || !corpusFile.exists()) {
      badOptions(p, "Corpus does not exist");
    }
    val outputTopics = os.valueOf("t").asInstanceOf[File]
    if(outputTopics == null) {
      badOptions(p, "Output not specified");
    }
    val outputDocTopics = os.valueOf("o").asInstanceOf[File]
    if(outputDocTopics == null) {
      badOptions(p, "Output not specified");
    }
    val mapper = new ObjectMapper();
    // Read configuration
    val config = mapper.readValue(configuration, classOf[Configuration])
    val corpus = mapper.readValue(corpusFile, classOf[Corpus])
    val searcher = DocumentSearcherFactory.loadSearcher(corpus);

    val extractor = new TopicExtraction(config)

    val (docTopics, topics) = extractor.extractTopics(searcher.allDocuments())

    mapper.writerWithDefaultPrettyPrinter().writeValue(outputTopics, asJavaIterable(topics))
    mapper.writerWithDefaultPrettyPrinter().writeValue(outputDocTopics, asJavaIterable(docTopics))

 }

  def mergeTopics(topicMap : java.util.Map[String, Topic], topics : Set[Topic]) {
    for(topic <- topics) {
      if(topicMap.containsKey(topic.topicString)) {
        val t3 = topicMap.get(topic.topicString);
        val mv2 = new java.util.ArrayList(topic.mvList);
        for(mvx <- t3.mvList) {
          if(!mv2.contains(mvx)) {
            mv2.add(mvx);
          }
        }
        val t2 = new Topic(topic.topicString, t3.occurrences + topic.occurrences, t3.matches + topic.matches, topic.score, mv2);
        topicMap.put(t2.topicString, t2);
      } else {
        topicMap.put(topic.topicString, topic);
      }
    }
  }

}

class Configuration {

  var threshold = 0.5
}
