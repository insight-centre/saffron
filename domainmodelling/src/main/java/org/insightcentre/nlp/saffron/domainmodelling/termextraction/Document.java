/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;

import java.util.Map;

/**
 * @author Georgeta Bordea
 *
 */
public class Document {
  
  private String id;
  private Map<String, Double> keyphraseRankMap;
  
  public Document(String id, Map<String, Double> keyphraseRankMap) {
    super();
    this.id = id;
    this.keyphraseRankMap = keyphraseRankMap;
  }
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public Map<String, Double> getKeyphraseRankMap() {
    return keyphraseRankMap;
  }
  public void setKeyphraseRankMap(Map<String, Double> keyphraseRankMap) {
    this.keyphraseRankMap = keyphraseRankMap;
  }
}
