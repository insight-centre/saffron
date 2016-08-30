/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.posextraction;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.domainmodelling.CorpusWord;
import org.insightcentre.nlp.saffron.domainmodelling.CorpusWordList;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.Keyphrase;
import org.insightcentre.nlp.saffron.domainmodelling.util.SaffronMapUtils;

/**
 * @author Georgeta Bordea
 * 
 */
public class ExtractionResultsWrapper {

  private Map<String, Keyphrase> nounPhraseMap = new HashMap<String, Keyphrase>();
  private Map<String, Long> verbsMap = new HashMap<String, Long>();
  private Map<String, Long> adjsMap = new HashMap<String, Long>();
  private Map<String, Long> nounsMap = new HashMap<String, Long>();
  private Map<String, Integer> docsLengthMap = new HashMap<String, Integer>();

  private Integer docCount;
  private Integer tokensNo = 0;

  public ExtractionResultsWrapper(Map<String, Keyphrase> nounPhraseMap,
      Map<String, Long> verbsMap, Map<String, Long> adjsMap,
      Map<String, Long> nounsMap, Integer docCount, Integer tokensNo) {
    super();
    this.nounPhraseMap = nounPhraseMap;
    this.verbsMap = verbsMap;
    this.adjsMap = adjsMap;
    this.nounsMap = nounsMap;
    this.docCount = docCount;
    this.tokensNo = tokensNo;
  }

  public Map<String, Keyphrase> getNounPhraseMap() {
    return nounPhraseMap;
  }

  public void setNounPhraseMap(Map<String, Keyphrase> stringMap) {
    this.nounPhraseMap = stringMap;
  }

  public Integer getDocCount() {
    return docCount;
  }

  public void setDocCount(Integer docCount) {
    this.docCount = docCount;
  }

  public Integer getTokensNo() {
    return tokensNo;
  }

  public void setTokensNo(Integer tokensNo) {
    this.tokensNo = tokensNo;
  }

  public Map<String, Long> getVerbsMap() {
    return verbsMap;
  }

  public void setVerbsMap(Map<String, Long> verbsMap) {
    this.verbsMap = verbsMap;
  }

  public Map<String, Long> getAdjsMap() {
    return adjsMap;
  }

  public void setAdjsMap(Map<String, Long> adjsMap) {
    this.adjsMap = adjsMap;
  }

  public Map<String, Long> getNounsMap() {
    return nounsMap;
  }

  public void setNounsMap(Map<String, Long> nounsMap) {
    this.nounsMap = nounsMap;
  }
  
  public Map<String, Integer> getDocsLengthMap() {
    return docsLengthMap;
  }

  public void setDocsLengthMap(Map<String, Integer> docsLengthMap) {
    this.docsLengthMap = docsLengthMap;
  }

  public Map<String, Long> mergeMap(Map<String, Long> srcMap,
      Map<String, Long> destMap, Boolean sumValues) {

    Set<String> keySet = srcMap.keySet();
    for (String key : keySet) {
      if (!destMap.containsKey(key)) {
        destMap.put(key, srcMap.get(key));
      } else if (sumValues) {
        destMap.put(key, destMap.get(key) + 1);
      }
    }

    return destMap;
  }
  
  public Map<String, Keyphrase> mergeNPMap(Map<String, Keyphrase> srcMap,
      Map<String, Keyphrase> destMap) {

    Set<String> keySet = srcMap.keySet();
    for (String key : keySet) {
      if (!destMap.containsKey(key)) {
        destMap.put(key, srcMap.get(key));
      }
    }

    return destMap;
  }

  public static CorpusWordList asCorpusWords(Map<String, Long> map) {
      map = SaffronMapUtils.sortByValues(map);

      List<CorpusWord> corpusWords = new ArrayList<>();
      for (Map.Entry<String, Long> o : map.entrySet()) {
          CorpusWord c = new CorpusWord();
          c.setDocumentFrequency(o.getValue());
          c.setString(o.getKey());
          corpusWords.add(c);
      }

      CorpusWordList corpusWordList = new CorpusWordList();
      corpusWordList.setWords(corpusWords);
      return corpusWordList;
  }
}
