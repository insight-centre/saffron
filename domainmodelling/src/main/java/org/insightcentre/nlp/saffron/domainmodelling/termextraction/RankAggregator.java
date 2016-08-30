/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.domainmodelling.util.SaffronMapUtils;

/**
 * @author Georgeta Bordea
 * 
 */
public class RankAggregator {

  protected static Map<String, Integer> bordaCount(List<String> kpList,
      List<Map<String, Integer>> ranksMapList) {
    Map<String, Integer> finalMap = new HashMap<String, Integer>();

    for (Map<String, Integer> ranksMap : ranksMapList) {
      for (String kp : kpList) {
        Integer cnt = countLargerDistinctRanks(kp, ranksMap);

        if (finalMap.containsKey(kp)) {
          finalMap.put(kp, finalMap.get(kp) + cnt);
        } else {
          finalMap.put(kp, cnt);
        }
      }
    }

    finalMap = SaffronMapUtils.sortByValues(finalMap);

    return finalMap;
  }

  protected static Map<String, Integer> computeRanksMap(
      Map<String, Double> scoresMap) {
    Map<String, Integer> ranksMap = new HashMap<String, Integer>();

    Integer rank = 1;
    scoresMap = SaffronMapUtils.sortByValues(scoresMap);
    List<String> kpList = new ArrayList<String>(scoresMap.keySet());
    Collections.reverse(kpList);

    Double minScore = scoresMap.get(kpList.get(0));

    for (String kp : kpList) {
      if (scoresMap.get(kp) < minScore) {
        rank++;
        minScore = scoresMap.get(kp);
        ranksMap.put(kp, rank);
      } else {
        ranksMap.put(kp, rank);
      }
    }

    return ranksMap;
  }

  private static Integer countLargerDistinctRanks(String kp,
      Map<String, Integer> ranksMap) {
    Set<String> kpSet = ranksMap.keySet();

    Integer kpRank = ranksMap.get(kp);
    Integer cnt = 0;
    Integer distinctRank = ranksMap.get(kp);
    for (String key : kpSet) {
      if ((kpRank < ranksMap.get(key)) && (distinctRank != ranksMap.get(key))) {
        cnt++;
        distinctRank = ranksMap.get(key);
      }
    }

    return cnt;
  }
}
