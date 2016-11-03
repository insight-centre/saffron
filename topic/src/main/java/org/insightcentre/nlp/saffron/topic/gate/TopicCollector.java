package org.insightcentre.nlp.saffron.topic.gate;

import gate.Annotation;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleResource;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.topic.ExtractedTopic;
import static org.insightcentre.nlp.saffron.topic.gate.GateProcessorConsumer.TOPICS_LIST_FEATURE_NAME;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
@CreoleResource(name = "Topic Collector", comment = "Resource used to collect the extracted topics")
public class TopicCollector extends AbstractLanguageAnalyser implements
    ProcessingResource, Serializable {

  /**
	 * 
	 */
  private static final long serialVersionUID = -6526769123531395877L;

  public TopicCollector() {
  }

  @Override
  public Resource init() throws ResourceInstantiationException {
      super.init();

    return this;
  }

  @Override
  public void execute() {

    /**
     * we store the identified topics for the document here
     */
    List<ExtractedTopicImpl> extractedTopics =
        new ArrayList<ExtractedTopicImpl>();

    // we store the numbers of tokens in the document here
    Integer tokensNumber = 0;

    // we collect and count the topics here

    // get the annotationSet name provided by the user, or fail

    AnnotationSet topics = null;
    AnnotationSet tokens = null;
    AnnotationSet acronyms = null;
    AnnotationSet expansions = null;

    try {
      topics = document.getAnnotations().get(topicAnnotationSetName);
      tokens = document.getAnnotations().get(TOKEN_ANNOTATION_TYPE);
      expansions = document.getAnnotations().get(EXPANSION_ANNOTATION_SET_NAME);
      if (tokens != null) {
        tokensNumber = tokens.size();
      }
    } catch (Exception exn) {
      throw new RuntimeException(exn);
    }

    // build the map with the acronyms and expanded acronyms
    Map<String, String> expMap = new HashMap<String, String>();

    if (expansions != null) {
      Iterator<Annotation> expItr = expansions.iterator();
      while (expItr.hasNext()) {
        Annotation expAnnot = expItr.next();
        FeatureMap topicFeatures = expAnnot.getFeatures();
        String expandedAcronym =
            (String) topicFeatures.get(EXPANDED_ACRONYM_FEATURE_NAME);
        String acronym = (String) topicFeatures.get(ACRONYM_FEATURE_NAME);
        expMap.put(expandedAcronym, acronym);
        // logger.log(Level.INFO, acronym + " -> " + expandedAcronym);
      }
    }

    // build the map with the topic annotations and features
    if (topics != null) {
      Iterator<Annotation> topicItr = topics.iterator();
      while (topicItr.hasNext()) {
        Annotation topicAnnot = topicItr.next();
        FeatureMap topicFeatures = topicAnnot.getFeatures();

        String topicString =
            (String) topicFeatures.get(topicTextualRepresentationFeatureName);
        String context =
            (String) topicFeatures
                .get(SEGMENT_TEXTUAL_REPRESENTATION_FEATURE_NAME);
        String pattern = (String) topicFeatures.get(POS_SEQUENCE_FEATURE_NAME);
        String rootSequence =
            (String) topicFeatures.get(ROOT_SEQUENCE_FEATURE_NAME);
        String contextPattern =
            (String) topicFeatures
                .get(CONTEXT_TEXTUAL_REPRESENTATION_FEATURE_NAME);
        Long offset = (Long) topicFeatures.get(OFFSET_FEATURE_NAME);

        String expandedTopic = null;
        String acronym = null;

        Set<String> keySet = expMap.keySet();
        for (String key : keySet) {
          String expTopicCandidate = key;
          String acronymCandidate = expMap.get(key);
          if (topicString.equalsIgnoreCase(expTopicCandidate)) {
            acronym = acronymCandidate;
          }
          if (topicString.equals(acronymCandidate)) {
            expandedTopic = expTopicCandidate;
          }
        }

        ExtractedTopicImpl extractedTopic =
            new ExtractedTopicImpl(topicString, context, pattern,
                contextPattern, rootSequence, acronym, expandedTopic, offset);

        extractedTopics.add(extractedTopic);
      }
    }

    // adding to gate.document
    document.getFeatures().put(topicsListFeatureName == null ? TOPICS_LIST_FEATURE_NAME : topicsListFeatureName, extractedTopics);
    document.getFeatures().put(tokensNumberFeatureName == null ? TOKEN_STRING_FEATURE_NAME : tokensNumberFeatureName, tokensNumber);
  }

  /*
   * GETTERS AND SETTERS
   */

  public String getTopicTextualRepresentationFeatureName() {
    return topicTextualRepresentationFeatureName;
  }

  public void setTopicTextualRepresentationFeatureName(
      String topicTextualRepresentationFeatureName) {
    this.topicTextualRepresentationFeatureName =
        topicTextualRepresentationFeatureName;
  }

  /**
   * Set the name of the annotation set to place the generated Token annotations
   * in.
   */
  public void setTopicAnnotationSetName(String annotationSetName) {
    this.topicAnnotationSetName = annotationSetName;
  }

  /**
   * Return the annotation set name used for the Tokens.
   */
  public String getTopicAnnotationSetName() {
    return topicAnnotationSetName;
  }

  public String getTokensNumberFeatureName() {
    return tokensNumberFeatureName;
  }

  public void setTokensNumberFeatureName(String tokensNumberFeatureName) {
    this.tokensNumberFeatureName = tokensNumberFeatureName;
  }

  public String getTopicsListFeatureName() {
    return topicsListFeatureName;
  }

  public void setTopicsListFeatureName(String topicsListFeatureName) {
    this.topicsListFeatureName = topicsListFeatureName;
  }

  private String topicTextualRepresentationFeatureName = "topicTextualRepresentation";
  private String topicAnnotationSetName;
  private String tokensNumberFeatureName;// = "tokensNumber";
  private String topicsListFeatureName;// = "topicsList";

  private static final String POS_SEQUENCE_FEATURE_NAME = "POSsequence";
  private static final String ROOT_SEQUENCE_FEATURE_NAME = "RootSequence";
  private static final String CONTEXT_TEXTUAL_REPRESENTATION_FEATURE_NAME =
      "contextTextualRepresentation";
  private static final String SEGMENT_TEXTUAL_REPRESENTATION_FEATURE_NAME =
      "segmentTextualRepresentation";
  private static final String EXPANSION_ANNOTATION_SET_NAME = "Expansion";
  private static final String EXPANDED_ACRONYM_FEATURE_NAME = "matchedTokens";
  private static final String ACRONYM_FEATURE_NAME = "acronym";
  private static final String OFFSET_FEATURE_NAME = "offset";

  private static class ExtractedTopicImpl implements ExtractedTopic {
        String topicString;
        String context;
        String pattern;
        String contextPattern;
        String rootSequence;
        String acronym;
        String expandedTopic;
        long offset;

        public ExtractedTopicImpl(String topicString, String context, String pattern, String contextPattern, String rootSequence, String acronym, String expandedTopic, long offset) {
            this.topicString = topicString;
            this.context = context;
            this.pattern = pattern;
            this.contextPattern = contextPattern;
            this.rootSequence = rootSequence;
            this.acronym = acronym;
            this.expandedTopic = expandedTopic;
            this.offset = offset;
        }

        public String getTopicString() {
            return topicString;
        }

        public void setTopicString(String topicString) {
            this.topicString = topicString;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getContextPattern() {
            return contextPattern;
        }

        public void setContextPattern(String contextPattern) {
            this.contextPattern = contextPattern;
        }

        public String getRootSequence() {
            return rootSequence;
        }

        public void setRootSequence(String rootSequence) {
            this.rootSequence = rootSequence;
        }

        public String getAcronym() {
            return acronym;
        }

        public void setAcronym(String acronym) {
            this.acronym = acronym;
        }

        public String getExpandedAcronym() {
            return expandedTopic;
        }

        public void setExpandedAcronym(String expandedTopic) {
            this.expandedTopic = expandedTopic;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        @Override
        public int compareTo(ExtractedTopic o) {
            return Long.compare(offset, o.getOffset());
        }
        
        
        
  }
} // class TopicCollector
