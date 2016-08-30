/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.posextraction;


/**
 * @author Georgeta Bordea
 * 
 */
public interface StatProcessor {
  ExtractionResultsWrapper extractNLPInfo(String docPath,
      Integer lengthThreshold);
}
