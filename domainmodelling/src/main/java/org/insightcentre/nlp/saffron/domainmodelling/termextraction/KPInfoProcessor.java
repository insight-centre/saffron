/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;

import ie.deri.unlp.javaservices.documentindex.DocumentSearcher;
import ie.deri.unlp.javaservices.documentindex.SearchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.insightcentre.nlp.saffron.domainmodelling.util.FilterUtils;
import org.insightcentre.nlp.saffron.domainmodelling.util.SaffronMapUtils;
import org.insightcentre.nlp.saffron.domainmodelling.util.StemUtils;

/**
 * @author Georgeta Bordea
 * 
 */
public class KPInfoProcessor {

	private DocumentSearcher lp;

	public KPInfoProcessor(DocumentSearcher lp) throws IOException {
		super();
		this.lp = lp;
	}

	/**
	 * Count how many other keyphrases embed a given termextraction
	 */
	protected static Integer computeEmbeddedness(DocumentSearcher lp, String keyphrase, Set<String> keyset) {

		try {
			keyphrase = parseKPLucene(lp, keyphrase);
		} catch (IOException e) {
			//logger.log(Level.WARN, "Could not parse with index the term " + keyphrase);
			e.printStackTrace();
		}

		Integer emb = 0;
		for (String key : keyset) {
			if ((key.contains(" " + keyphrase + " "))
					|| (key.startsWith(keyphrase + " ") || (key.endsWith(" " + keyphrase)))) {
				emb++;
			}
		}

		return emb;
	}

	/**
	 * Compute the taxons similarity for all keyphrases, discard keyphrases that
	 * have a rank below the given threshold
	 * 
	 */
	public Map<String, Keyphrase> computeTokenSimilarity(DocumentSearcher metrics, Map<String, Keyphrase> kpMap, List<String> taxons,
			Integer taxSimLengthThresh, Double rankThreshold, Integer corpusFreqTaxSim, Integer docsCount,
			Integer boost, Long tokensInCorpus, Integer spanSlop) throws SearchException {

		Map<String, Keyphrase> kpResultsMap = new HashMap<String, Keyphrase>();
		Map<String, Long> taxonsFreqMap = loadTaxonsFrequencies(taxons);

		TreeSet<String> keySet = new TreeSet<String>(kpMap.keySet());
		for (String key : keySet) {
			Keyphrase kp = kpMap.get(key);
			if ((kp.getLength() > taxSimLengthThresh) && (kp.getRank() >= rankThreshold)
					&& (kp.getFrequency() > corpusFreqTaxSim)) {
				kp.setTaxonsSimilarity(TaxonSimilarity.computeTaxonSpanSimilarity(metrics, key, kp.getFrequency(),
						taxonsFreqMap, docsCount, boost, tokensInCorpus, spanSlop));
				kpResultsMap.put(key, kp);
			} else {
				kp.setTaxonsSimilarity(0.0);
				kpResultsMap.put(key, kp);
			}
		}

		return kpMap;
	}

	protected Map<String, Long> loadTaxonsFrequencies(List<String> taxons) throws SearchException {
		Map<String, Long> taxonFreqMap = new HashMap<String, Long>();

		for (String taxon : taxons) {
			Long taxonFreq = lp.numberOfOccurrences(taxon, lp.numDocs());
			taxonFreqMap.put(taxon, taxonFreq);
		}

		return taxonFreqMap;
	}

	/**
	 * Filter the keyphrases for embeddedness computation, only keyphrases
	 * longer than 2 words can embed a 2 word termextraction
	 * 
	 * @param kpMap
	 * @return
	 * @throws IOException
	 */
	protected static Set<String> extractParsedEmbKP(DocumentSearcher lp, Map<String, Keyphrase> kpMap, Integer threshold) throws IOException {

		Set<String> outputKeySet = new HashSet<String>();

		for (String key : kpMap.keySet()) {
			if (kpMap.get(key).getLength() >= threshold) {

				if (!outputKeySet.contains(key)) {
					String resultKey = parseKPLucene(lp, key);

					if (resultKey != null) {
						outputKeySet.add(resultKey);
					}
				}
			}
		}

		return outputKeySet;
	}

	protected static Set<String> extractEmbKP(Map<String, Keyphrase> kpMap, Integer threshold) throws IOException {

		Set<String> keySet = kpMap.keySet();

		Set<String> outputKeySet = new HashSet<String>();

		for (String key : keySet) {
			if (kpMap.get(key).getLength() >= threshold) {
				outputKeySet.add(key);
			}
		}

		return outputKeySet;
	}

	public static String parseKPLucene(DocumentSearcher lp, String key) throws IOException {
		List<String> terms = lp.analyseTerm(key);
		if (terms.size() == 0) {
			return null;
		}
		return StringUtils.join(terms, " ");
	}

	/**
	 * Assign keyphrases to documents and compute ranks
	 * 
	 * @param keyphraseMap
	 * @param docCount
	 * @return
	 */
	protected Map<String, Document> assignKeyphrasesToDocs(Boolean allDocs, Map<String, Keyphrase> keyphraseMap,
			Integer docCount, Double rankThreshold) {

		Map<String, Document> docMap = new HashMap<String, Document>();

		Set<String> keySet = keyphraseMap.keySet();

		for (String keyphrase : keySet) {

			try {

				Keyphrase keyphraseObj = keyphraseMap.get(keyphrase);
				docMap = assignKPtoDoc(allDocs, docCount, docMap, keyphrase, keyphraseObj.getRank(), rankThreshold);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return docMap;
	}

	public Map<String, Document> assignKpSimToDocsFromFile(Boolean allDocs, Map<String, Keyphrase> kpMap,
			Double rankThreshold, Set<String> stopWords) throws IOException, SearchException {
		TreeSet<String> keySet = new TreeSet<String>(kpMap.keySet());
		Map<String, Document> docMap = new HashMap<String, Document>();

		for (String keyphrase : keySet) {
			Keyphrase keyphraseObj = kpMap.get(keyphrase);
			if (FilterUtils.isProperTopic(keyphrase, stopWords) && keyphraseObj.getRank() > 0) {

				docMap = assignKPtoDoc(allDocs, lp.numDocs(), docMap, keyphrase, keyphraseObj.getRank(), rankThreshold);

			}
		}

		return docMap;
	}

	public Map<String, Document> assignKpToDocsFromFile(Boolean allDocs, Map<String, Keyphrase> kpMap,
			Double rankThreshold, Set<String> stopWords) throws SearchException {

		TreeSet<String> keySet = new TreeSet<String>(kpMap.keySet());

		Map<String, Double> kpRanksMap = new HashMap<String, Double>();
		for (String kp : keySet) {
			kpRanksMap.put(kp, kpMap.get(kp).getRank());
		}

		// only extract top keyphrases, it takes too long to consider all
		List<String> topKPList = KPInfoProcessor.cutTopKpUniqueRoot(kpRanksMap, kpMap.size() / 5); // /
																									// 1);
																									// /
																									// 5);
		Collections.sort(topKPList);

		Map<String, Document> docMap = new HashMap<String, Document>();

		for (String keyphrase : topKPList) {
			// System.out.println(termextraction);
			Keyphrase keyphraseObj = kpMap.get(keyphrase);
			if (FilterUtils.isProperTopic(keyphrase, stopWords) && keyphraseObj.getRank() > 0) {
				try {
					docMap = assignKPtoDoc(allDocs, lp.numDocs(), docMap, keyphrase, keyphraseObj.getRank(),
							rankThreshold);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return docMap;
	}

	protected static List<String> cutTopKp(Map<String, Double> keyphraseMap, Integer upto) {
		keyphraseMap = SaffronMapUtils.sortByValues(keyphraseMap);

		List<String> kpList = new ArrayList<String>();
		Set<String> keyphraseSet = keyphraseMap.keySet();
		for (String keyphrase : keyphraseSet) {
			kpList.add(keyphrase);
		}

		Collections.reverse(kpList);

		Integer top = upto;
		if (top > kpList.size()) {
			top = kpList.size();
		}

		kpList = kpList.subList(0, top);

		return kpList;
	}

	protected static List<String> cutTopKpUniqueRoot(Map<String, Double> keyphraseMap, Integer upto)
			throws SearchException {
		keyphraseMap = SaffronMapUtils.sortByValues(keyphraseMap);

		List<String> kpList = new ArrayList<String>();
		Set<String> keyphraseSet = keyphraseMap.keySet();
		for (String keyphrase : keyphraseSet) {
			kpList.add(keyphrase);
		}

		Collections.reverse(kpList);

		Map<String, String> kpStemsMap = StemUtils.stemAll(kpList);
		List<String> uniqueStemsKPList = SaffronMapUtils.keysWithUniqueValues(kpStemsMap);

		List<String> tmpKPList = new ArrayList<String>();
		for (String key : kpList) {
			if (uniqueStemsKPList.contains(key)) {
				tmpKPList.add(key);
			}
		}

		Integer top = upto;
		if (top > tmpKPList.size()) {
			top = tmpKPList.size();
		}

		kpList = tmpKPList.subList(0, top);

		return kpList;
	}
	
	protected Map<String, Document> assignKPtoDoc(Boolean allDocs, Integer docCount, Map<String, Document> docMap,
			String keyphrase, Double rank, Double rankThresh) throws SearchException {
		Map<String, Float> tfidfMap = lp.tfidf(keyphrase, docCount);
		Set<String> tfidfKeySet = tfidfMap.keySet();

		for (String docId : tfidfKeySet) {
			keyphrase = keyphrase.toLowerCase();

			Document document = docMap.get(docId);

			Double finalRank = 0.0;

			if ((rank > rankThresh)) {
				finalRank = rank * new Double(tfidfMap.get(docId));

				// check if the document is empty, only apply zone relevance
				// otherwise
				/*
				 * if (content.length() > 0) { Integer offset =
				 * content.indexOf(termextraction); Integer offsetZone = offset
				 * / (content.length() / 10); finalRank =
				 * applyDocZoneRelevance(finalRank, offsetZone); }
				 */
			}

			// finalRank = new Double(tfidfMap.get(docId));

			if (document != null) {
				Map<String, Double> rankMap = document.getKeyphraseRankMap();

				if (!rankMap.containsKey(keyphrase)) {

					rankMap.put(keyphrase, finalRank);
				}

				document.setKeyphraseRankMap(rankMap);
			} else {
				Map<String, Double> rankMap = new HashMap<String, Double>();
				rankMap.put(keyphrase, finalRank);
				Document docObj = new Document(docId, rankMap);
				docMap.put(docId, docObj);
			}
		}

		return docMap;
	}


	public Map<String, Keyphrase> computeRanks(Integer rankType, Map<String, Keyphrase> kpMap, Double alpha,
			Double beta, int lengthThreshold) throws IOException {

		Set<String> keySet = kpMap.keySet();

		Long maxFreq = Keyphrase.computeMaxFreq(kpMap);
		Integer maxEmb = Keyphrase.computeMaxEmb(kpMap);

		List<String> kpList = new ArrayList<String>(keySet);
		Collections.sort(kpList);

		for (String key : kpList) {
			Keyphrase kp = kpMap.get(key);

			switch (rankType) {
			case 0:
				// Saffron baseline
				kp.setRank(kp.computeRank(maxFreq, maxEmb, alpha));
				break;
			case 1:
				// DSR
				Double maxTaxSim = Keyphrase.computeMaxTaxSim(kpMap);
				kp.setRank(kp.computeSimRank(maxFreq, maxEmb, maxTaxSim, alpha, beta));
				break;
			case 2:
				// C-value
				kp.setRank(kp.computeCValueRank(kpMap, lengthThreshold, lp.numDocs()));
				break;
			default:
				throw new RuntimeException("Unrecognised rank type!");
			}

			kpMap.put(key, kp);
		}

		return kpMap;
	}
}
