/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Georgeta Bordea
 * 
 */
public class Keyphrase {

	private Integer length;
	private Long frequency;
	private Integer embeddedness;
	private Double taxonsSimilarity;
	private Long ybhits;
	private String string;

	private Double rank = 0.0;

	public Keyphrase(String string, Integer length, Long frequency, Integer embeddedness, Long ybhits,
			Double taxonsSimilarity) {
		super();
		this.length = length;
		this.frequency = frequency;
		this.embeddedness = embeddedness;
		this.taxonsSimilarity = taxonsSimilarity;
		this.ybhits = ybhits;
		this.string = string;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	public Long getFrequency() {
		return frequency;
	}

	public void setFrequency(Long frequency) {
		this.frequency = frequency;
	}

	public Integer getEmbeddedness() {
		return embeddedness;
	}

	public void setEmbeddedness(Integer embeddedness) {
		this.embeddedness = embeddedness;
	}

	public Double getTaxonsSimilarity() {
		return taxonsSimilarity;
	}

	public void setTaxonsSimilarity(Double taxonsSimilarity) {
		this.taxonsSimilarity = taxonsSimilarity;
	}

	public Double getRank() {
		return rank;
	}

	@Override
	public String toString() {
		return "Keyphrase [string=" + string + "]";
	}

	public Double computeRank(Long maxFreq, Integer maxEmb, Double alpha) {
		Double freq = new Double(frequency);
		Double emb = new Double(embeddedness);
		Double l = new Double(length);

		Double partialRank = l * Math.log1p(freq);
		Double baselineRank = partialRank + alpha * Math.log1p(emb);
		return baselineRank;
	}

	public Double computeCValueRank(Map<String, Keyphrase> kpMap, Integer maxLength, Integer docCount)
			throws IOException {

		Set<String> embKPSet;
		embKPSet = KPInfoProcessor.extractEmbKP(kpMap, 2);

		Double freq = new Double(frequency);
		Double l = new Double(length);
		Map<String, Long> embMap = computeEmbeddednessMap(string, maxLength, embKPSet, kpMap, docCount);

		Double rank = Math.log(l) * freq;
		if (embMap.size() == 0) {
			return rank;
		} else {
			Set<String> embKps = embMap.keySet();
			Long freqSum = new Long(0);
			for (String embKp : embKps) {
				freqSum += embMap.get(embKp);
			}

			return rank - freqSum / embMap.size();
		}
	}

	private Map<String, Long> computeEmbeddednessMap(String keyphrase, Integer maxLength, Set<String> keySet,
			Map<String, Keyphrase> kpMap, Integer docCount) {

		Map<String, Long> embMap = new HashMap<String, Long>();

		if (kpMap.get(keyphrase).getLength() < maxLength) {
			for (String key : keySet) {
				if ((key.contains(" " + keyphrase + " "))
						|| (key.startsWith(keyphrase + " ") || (key.endsWith(" " + keyphrase)))) {
					embMap.put(key, kpMap.get(key).getFrequency());
				}
			}
		}

		return embMap;
	}

	public void setRank(Double rank) {
		this.rank = rank;
	}

	public Double computeSimRank(Long maxFreq, Integer maxEmb, Double maxTaxSim, Double alpha, Double beta) {
		// this.rank =
		// this.getLength() * Math.log(1 + this.getFrequency());

		Double freq = new Double(frequency);
		Double emb = new Double(embeddedness);
		Double l = new Double(length);

		Double partialRank = l * Math.log1p(freq);

		// best alpha = 1000
		// Double baselineRank = (1 - alpha) * partialRank + alpha * Math.log(1
		// +
		// emb);
		Double baselineRank = partialRank + alpha * Math.log1p(emb);

		// if (taxonsSimilarity > 0) {
		// this.rank = l * Math.log(1 + freq) + 3.5 * Math.log(1 + emb);
		// } else {
		// this.rank = 0.0;
		// }

		// this.rank = l * freq / maxFreq + 3.5 * emb / maxEmb +
		// taxonsSimilarity
		// / maxTaxSim;

		// this.rank = baselineRank * taxonsSimilarity;

		// if (taxonsSimilarity > KPUtils.TAXON_SIMILARITY_THRESHOLD) {
		// this.rank = baselineRank;
		// } else {
		// this.rank = 0.0;
		// }

		// BEST - similarity pxy with boost and slop 2
		// this.rank = baselineRank + beta * l * Math.log1p(taxonsSimilarity);

		// if (length > 1) {
		// this.rank = baselineRank;
		// } else {
		return baselineRank + beta * Math.log1p(taxonsSimilarity);
		// }

		// this.rank = partialRank;
	}

	public Long getYbhits() {
		return ybhits;
	}

	public void setYbhits(Long ybhits) {
		this.ybhits = ybhits;
	}

	public static Long computeMaxFreq(Map<String, Keyphrase> kpMap) {
		Long max = new Long(0);
		Set<String> keySet = kpMap.keySet();

		for (String key : keySet) {
			Long freq = kpMap.get(key).getFrequency();
			if (freq > max) {
				max = freq;
			}
		}
		return max;
	}

	public static Integer computeMaxEmb(Map<String, Keyphrase> kpMap) {
		Integer max = 0;
		Set<String> keySet = kpMap.keySet();

		for (String key : keySet) {
			Integer emb = kpMap.get(key).getEmbeddedness();
			if (emb > max) {
				max = emb;
			}
		}
		return max;
	}

	public static Double computeMaxTaxSim(Map<String, Keyphrase> kpMap) {
		Double max = 0.0;
		Set<String> keySet = kpMap.keySet();

		for (String key : keySet) {
			Double taxSim = kpMap.get(key).getTaxonsSimilarity();
			if (taxSim > max) {
				max = taxSim;
			}
		}
		return max;
	}
}
