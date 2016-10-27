/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.posextraction;

import java.util.List;
import java.util.Map;

/**
 * @author Georgeta Bordea
 *
 */
public interface POSBearer {
  
  public void setNounList(List<String> nounList);
  
  public List<String> getNounList();
  
  public void setVerbList(List<String> verbList);
  
  public List<String> getVerbList();
  
  public void setAdjectiveList(List<String> adjectiveList);
  
  public List<String> getAdjectiveList();
  
  public void setNounphraseMap(Map<String, Integer> nounphraseMap);
  
  public Map<String, Integer> getNounphraseMap();
  
  public void setTokensNo(Integer tokensNo);
  
  public Integer getTokensNo();
}
