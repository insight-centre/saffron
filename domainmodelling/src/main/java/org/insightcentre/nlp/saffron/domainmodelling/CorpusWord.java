package org.insightcentre.nlp.saffron.domainmodelling;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcenter.nlp.saffron.documentindex.SearchException;

@XmlRootElement
public class CorpusWord {

	private String string;
	private Long docDistribution;
	private Long ybhits;
	private Long documentFrequency;
	private Double rank;

	public CorpusWord(String string, Long docDistribution, Long ybhits, Long frequency) {
		super();
		this.string = string;
		this.docDistribution = docDistribution;
		this.ybhits = ybhits;
		this.documentFrequency = frequency;
	}

	public CorpusWord() {

	}

	public CorpusWord(String string, Double rank) {
		this.string = string;
		this.rank = rank;
	}

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public Long getDocDistribution() {
		return docDistribution;
	}

	public void setDocDistribution(Long docDistribution) {
		this.docDistribution = docDistribution;
	}

	public Long getYbhits() {
		return ybhits;
	}

	public void setYbhits(Long ybhits) {
		this.ybhits = ybhits;
	}

	public Long getDocumentFrequency() {
		return documentFrequency;
	}

	public void setDocumentFrequency(Long frequency) {
		this.documentFrequency = frequency;
	}

	public Double getRank() {
		return rank;
	}

	public void setRank(Double rank) {
		this.rank = rank;
	}

	protected static Integer computeEmb(String topKPFile, String word) {
		List<String> topKPList = TaxonUtils.readWordsFromFile(topKPFile);

		Integer emb = 0;
		for (String kp : topKPList) {
			if (kp.contains(" " + word + " ") || kp.startsWith(word + " ") || kp.endsWith(" " + word)) {
				emb++;
			}
		}
		return emb;
	}

	protected static Double contextKP(DocumentSearcher lp, String topKPFile, String word) throws IOException,
			SearchException {
		List<String> topKPList = TaxonUtils.readWordsFromFile(topKPFile);

		Double contextWordsRank = 0.0;

		for (String kp : topKPList) {

			Long spanFreq = lp.spanOccurrence(word, kp, TaxonUtils.DOCS_NO, 2);

			if (spanFreq > 0) {
				contextWordsRank++;
			}
		}

		return contextWordsRank / topKPList.size();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CorpusWord that = (CorpusWord) o;

		if (!string.equals(that.string))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return string.hashCode();
	}

	public static Double contextPMIkp(DocumentSearcher lp, List<String> topKPList, String word, Long freq)
			throws IOException, SearchException {
		Double contextWordsRank = 0.0;
		for (String kp : topKPList) {
			Long spanFreq = lp.spanOccurrence(word, kp, TaxonUtils.DOCS_NO, 5);
			Long kpFreq = lp.numberOfOccurrences(kp, TaxonUtils.DOCS_NO);

			double pxy = (double) spanFreq / TaxonUtils.TOKENS_NO;
			double px = (double) kpFreq / TaxonUtils.TOKENS_NO;
			double py = (double) freq / TaxonUtils.TOKENS_NO;

			if (spanFreq > 0) {
				contextWordsRank += Math.log(pxy / (px * py));
			}
		}

		return contextWordsRank / topKPList.size();
	}

	protected static Double contextMutualDependencyKP(DocumentSearcher lp, String topKPFile, String word, Long freq)
			throws IOException, SearchException {
		List<String> topKPList = TaxonUtils.readWordsFromFile(topKPFile);

		Double contextWordsRank = 0.0;
		for (String kp : topKPList) {

			Long spanFreq = lp.spanOccurrence(word, kp, TaxonUtils.DOCS_NO, 5);
			Long kpFreq = lp.numberOfOccurrences(kp, TaxonUtils.DOCS_NO);

			double pxy = (double) spanFreq / TaxonUtils.TOKENS_NO;
			double px = (double) kpFreq / TaxonUtils.TOKENS_NO;
			double py = (double) freq / TaxonUtils.TOKENS_NO;

			if (spanFreq > 0) {
				contextWordsRank += Math.log((pxy * pxy) / (px * py)) / Math.log(2) + Math.log(pxy) / Math.log(2);
			}
		}

		return contextWordsRank / topKPList.size();
	}

	protected static Double contextLogLikelihood(DocumentSearcher lp, String topKPFile, String word, Long freq)
			throws IOException, SearchException {
		List<String> topKPList = TaxonUtils.readWordsFromFile(topKPFile);

		Double contextWordsRank = 0.0;
		double n = TaxonUtils.TOKENS_NO - TaxonUtils.DOCS_NO * (TaxonUtils.SPAN_SLOP - 1);

		for (String kp : topKPList) {

			Long spanFreq = lp.spanOccurrence(word, kp, TaxonUtils.DOCS_NO, 5);
			Long kpFreq = lp.numberOfOccurrences(kp, TaxonUtils.DOCS_NO);

			double fxy = (double) spanFreq;
			// count number of ngrams, not words
			double fx = (double) kpFreq * TaxonUtils.SPAN_SLOP;
			double fy = (double) freq * TaxonUtils.SPAN_SLOP;
			double fx_y = fx - fxy;
			double f_xy = fy - fxy;
			double f_x_y = n - fx - fy + fxy;
			double fprimxy = (fxy + f_xy) * (fxy + fx_y) / n;

			if ((fprimxy > 0) && (fxy > 0) && (f_xy > 0) && (fx_y > 0) && (f_x_y > 0)) {
				contextWordsRank += -2
						* (fxy * Math.log(fxy / fprimxy) + fx_y * Math.log(fx_y / fprimxy) + f_xy
								* Math.log(f_xy / fprimxy) + f_x_y * Math.log(f_x_y / fprimxy));
			}
		}

		return -contextWordsRank / topKPList.size();
	}

	public static Double domainConsensus(DocumentSearcher sd, String word) throws SearchException {
		Double score = 0.0;

		Map<String, Integer> occMap;

		Map<String, Integer> docsLength = readDocumentsLengths(TaxonUtils.DOCS_LENGTH_FILE_NAME);

		occMap = sd.searchOccurrence(word, TaxonUtils.DOCS_NO);
		Set<String> keySet = occMap.keySet();
		for (String docId : keySet) {
			if (docsLength.get(docId) != null) {
				score += (double) occMap.get(docId) / docsLength.get(docId);
			}
		}

		return score;
	}

	protected static Map<String, Integer> readDocumentsLengths(String docFile) {
		Map<String, Integer> docsMap = new HashMap<String, Integer>();

		try {
			BufferedReader input = new BufferedReader(new FileReader(docFile));
			try {
				String line = null;
				while ((line = input.readLine()) != null) {
					docsMap.put(line.substring(0, line.indexOf(",")),
							Integer.parseInt(line.substring(line.lastIndexOf(",") + 1)));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return docsMap;
	}

	protected static Double normalisedDomainImpurity(DocumentSearcher sd, String word) throws IOException,
			SearchException {
		Double ndi = 0.0;

		Map<String, Integer> occMap;

		Map<String, Integer> docsLength = readDocumentsLengths(TaxonUtils.DOCS_LENGTH_FILE_NAME);

		occMap = sd.searchOccurrence(word, TaxonUtils.DOCS_NO);
		Set<String> keySet = occMap.keySet();

		Double domainFreq = 0.0;
		Double pdwSum = 0.0;

		for (String docId : keySet) {
			domainFreq += (double) occMap.get(docId);
		}

		for (String docId : keySet) {
			pdwSum += ((double) occMap.get(docId) / domainFreq) / docsLength.get(docId);
		}

		double pdw = 0.0;
		for (String docId : keySet) {

			pdw = ((double) occMap.get(docId) / domainFreq) / docsLength.get(docId);
			pdw = pdw / pdwSum;
			ndi += pdw * Math.log(pdw);
		}

		ndi = -ndi;

		return ndi;
	}

	/*
	 * Returns the probability that the word in the index is followed by
	 * for/to/on/of.
	 */
	protected static Double prepositionComposition(DocumentSearcher sd, String word, Long freq) throws IOException,
			SearchException {
		Double score = 0.0;
		String[] preps = { "for", "to", "on", "of" };

		Map<String, Integer> occMap;

		Long sumPrepOcc = new Long(0);
		for (String prep : preps) {
			occMap = sd.searchOccurrence(word + " " + prep, TaxonUtils.DOCS_NO);
			Set<String> keySet = occMap.keySet();

			for (String docId : keySet) {
				sumPrepOcc += occMap.get(docId);
			}
		}

		score = ((double) sumPrepOcc) / freq;
		return score;
	}

	protected static Double coocurrenceEntropy(DocumentSearcher sd, String candidate, List<CorpusWord> taxons) {
		Double entropy = 0.0;

		Integer sumCooc = 0;
		try {

			for (CorpusWord taxon : taxons) {
				Integer cooc = computeCoocurrence(sd, candidate, taxon.getString());
				sumCooc += cooc;
			}

			if (sumCooc > 0) {
				for (CorpusWord taxon : taxons) {
					Integer cooc = computeCoocurrence(sd, candidate, taxon.getString());
					Double pt1t2 = ((double) cooc) / sumCooc;

					if (pt1t2 > 0) {
						entropy += pt1t2 * Math.log(pt1t2);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -entropy;
	}

	/**
	 * Check how many document contain both words
	 * 
	 * @param word1
	 * @param word2
	 * @return the coocurrence of word1 and word2
	 */
	private static Integer computeCoocurrence(DocumentSearcher sd, String word1, String word2) throws IOException {
		Integer cooc = 0;

		try {
			Map<String, Integer> occMap1 = sd.searchOccurrence(word1, TaxonUtils.DOCS_NO);

			Map<String, Integer> occMap2 = sd.searchOccurrence(word2, TaxonUtils.DOCS_NO);

			Set<String> keys = occMap1.keySet();
			for (String key : keys) {
				if (occMap2.containsKey(key)) {
					cooc++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return cooc;
	}

	protected static Double subsumption(DocumentSearcher sd, String word, Long freq, List<CorpusWord> candidates) {
		Double subsump = 0.0;
		Double coocThreshold = 0.7;

		Double pw1w2 = 0.0;
		Double pw2w1 = 0.0;

		try {
			for (CorpusWord candidate : candidates) {

				Integer cooc = computeCoocurrence(sd, word, candidate.getString());
				pw1w2 = (double) cooc / candidate.getDocumentFrequency();
				pw2w1 = (double) cooc / freq;

				if ((pw1w2 >= coocThreshold) && (pw2w1 < coocThreshold)) {
					subsump++;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return subsump;
	}

	protected static List<CorpusWord> selectRankedCandidates(List<CorpusWord> candidates, String rankedFile) {
		List<String> topVelardi = TaxonUtils.readWordsFromFile(rankedFile);

		// only keep the Velardi top candidates
		List<CorpusWord> velardiCandidates = new ArrayList<CorpusWord>();
		for (CorpusWord candidate : candidates) {
			if (topVelardi.contains(candidate.getString())) {
				velardiCandidates.add(candidate);
			}
		}
		return velardiCandidates;
	}

	protected static Double windowSubsumption(DocumentSearcher lp, String word, Long freq, List<CorpusWord> candidates,
			Integer span) throws IOException, SearchException {
		Double subsump = 0.0;

		Double pw1w2 = 0.0;
		Double pw2w1 = 0.0;

		for (CorpusWord candidate : candidates) {

			Long cooc = lp.spanOccurrence(word, candidate.getString(), TaxonUtils.DOCS_NO, span);
			pw1w2 = (double) cooc / candidate.getDocumentFrequency();
			pw2w1 = (double) cooc / freq;

			if ((pw1w2 >= TaxonUtils.SUBSUMPTION_COOCURRENCE_THRESHOLD)
					&& (pw2w1 < TaxonUtils.SUBSUMPTION_COOCURRENCE_THRESHOLD)) {
				subsump++;
			}
		}

		return subsump;
	}

	protected static Double combinePrepWithPMIkpScore(String word, Double prepScore, Double pmiKPScore,
			Double prepThreshold) {
		Double score = 0.0;

		if (prepScore >= prepThreshold) {
			score = pmiKPScore;
		}

		return score;
	}

	protected static Double combineSubsumpWithPMIkpScore(String word, Double subsumpScore, Double pmiKPScore) {
		Double score = 0.0;
		score = pmiKPScore * (1 + Math.log(subsumpScore + 1));
		return score;
	}

}
