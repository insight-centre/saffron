/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.posextraction;

import java.net.MalformedURLException;

/**
 * @author Georgeta Bordea
 * 
 */
public interface POSExtractor {

  public static final String NOUN_LIST_FEATURE_NAME = "NOUN";
  public static final String VERB_LIST_FEATURE_NAME = "VERB";
  public static final String ADJECTIVE_LIST_FEATURE_NAME = "ADJ";
  
  public static final String CONTEXT_NOUN_LIST_FEATURE_NAME = "CNoun";
  
  public static final String NOUNPHRASE_MAP_FEATURE_NAME = "NounPhrase";
  
  public static final String TOKENS_NUMBER_FEATURE_NAME = "Token";

  /**
   * increase this in case multiple simultaneous requests are to be supported
   */
  public static final int NUMBER_OF_PIPELINES = 1;

  /**
   * Extracts Topics from a given text, referenced via URL. The URL must be
   * specified as string and better be valid.
   * 
   * @param documentUrl
   *          the url to extract the keywords from
   * @return a Map<String,Double>, where the keys represent keywords/keyphrases
   *         and the values represent significance (between [0;1]), ordered
   *         (hopefully!) by significance
   * @throws MalformedURLException
   *           in case the specified URL is not valid
   * @throws GateProcessorNotAvailableException
   *           in case all GateProcessors are busy/occupied
   * @throws UnsupportedFileTypeException
   *           in case the URL references a file type that is not supported
   */
  public POSBearer getPOSFromUrl(String documentUrl)
      throws MalformedURLException,
      UnsupportedFileTypeException, RuntimeException;

  /**
   * Extracts Topics from a given text, supplied as string.
   * 
   * @param documentText
   *          the string to extract the keywords from
   * @return a Map<String,Double>, where the keys represent keywords/keyphrases
   *         and the values represent significance (between [0;1]), ordered by
   *         significance
   * @throws GateProcessorNotAvailableException
   *           in case all GateProcessors are busy/occupied
   */
  public POSBearer getPOSFromText(String documentText)
      throws RuntimeException;
}
