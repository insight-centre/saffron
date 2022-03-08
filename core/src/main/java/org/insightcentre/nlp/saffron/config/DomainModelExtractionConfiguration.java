package org.insightcentre.nlp.saffron.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DomainModelExtractionConfiguration {

	public TermExtractionConfiguration seedTerms = new TermExtractionConfiguration();
	
	public int maxTerms = 100;
	public int ngramMin = 1;
	public int ngramMax = 1;
	public int maxDocs = Integer.MAX_VALUE;
	public double minDocFreq = 0.0;
	
	public Set<String> preceedingTokens = new HashSet<>(Arrays.asList("NN", "NNS", "JJ", "NNP"));
    /** The set of tags allowed in non-final position, but not completing */
    public Set<String> middleTokens = new HashSet<>(Arrays.asList("IN"));
    /** The set of final tags allows in a noun phrase */
    public Set<String> headTokens = new HashSet<>(Arrays.asList("NN", "NNS", "CD"));
    /** The position of the head of a noun phrase (true=final) */
    public boolean headTokenFinal = true;
}
