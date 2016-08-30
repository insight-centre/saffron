/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.posextraction;

/**
 * @author Georgeta Bordea
 * 
 */
public interface DocumentProcessor {
  public ExtractionResultsWrapper processDocument(String filePath,
      Integer lengthThreshold, ExtractionResultsWrapper erw, StatProcessor cs);
}
