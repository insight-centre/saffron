package org.insightcentre.nlp.saffron.term.domain;

import static java.lang.Integer.min;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.config.DomainModelExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration.Feature;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration.WeightingMethod;
import org.insightcentre.nlp.saffron.data.CollectionCorpus;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.term.TermExtraction;
import org.insightcentre.nlp.saffron.term.TermExtraction.Result;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

/**
 * Extracts a list of terms that represent the domain model
 * 
 * @author Bianca Pereira
 *
 */
public class DomainTermExtraction {
	
	private final TermExtractionConfiguration seedTermExtractionConfig;
	private final TermExtractionConfiguration dmExtractionConfig;
	
	private final TermExtraction te;
	private final TermExtraction dmCandidateExtraction;
	private final int maxDomainModelTerms;

	private final ThreadLocal<POSTagger> tagger;
    private final ThreadLocal<Tokenizer> tokenizer;
    private final ThreadLocal<Lemmatizer> lemmatizer;
		
	public DomainTermExtraction (DomainModelExtractionConfiguration config) throws IOException {
		this.seedTermExtractionConfig = config.seedTerms;
		this.te = new TermExtraction(config.seedTerms);
		
		TermExtractionConfiguration dmExtractionConfig = new TermExtractionConfiguration();
		dmExtractionConfig.maxTerms = 2000;// Ideally it should be Integer.MAX_VALUE, however it would be too 
		//computationally intensive to calculate PMI between all candidates and all seed terms
		this.maxDomainModelTerms = config.maxTerms;
		dmExtractionConfig.ngramMin = config.ngramMin;
		dmExtractionConfig.ngramMax = config.ngramMax;
		dmExtractionConfig.minDocFreq = config.minDocFreq;
		dmExtractionConfig.preceedingTokens = config.preceedingTokens;
		dmExtractionConfig.middleTokens = config.middleTokens;
		dmExtractionConfig.headTokens = config.headTokens;
		dmExtractionConfig.headTokenFinal = config.headTokenFinal;
		dmExtractionConfig.posModel = config.seedTerms.posModel;
		dmExtractionConfig.tokenizerModel = config.seedTerms.tokenizerModel;
		dmExtractionConfig.lemmatizerModel = config.seedTerms.lemmatizerModel;
		dmExtractionConfig.baseFeature = Feature.termFreq;
		dmExtractionConfig.method = WeightingMethod.one;
		dmExtractionConfig.oneTermPerDoc = false;
		this.dmExtractionConfig = dmExtractionConfig;
		this.dmCandidateExtraction = new TermExtraction(dmExtractionConfig);

		final TokenizerModel tokenizerModel;
        if (config.seedTerms.tokenizerModel == null) {
            tokenizerModel = null;
        } else {
            tokenizerModel = new TokenizerModel(config.seedTerms.tokenizerModel.toFile());
        }
		this.tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {

                if (tokenizerModel == null) {
                    return SimpleTokenizer.INSTANCE;
                } else {
                    return new TokenizerME(tokenizerModel);
                }
            }
        };
        
        final POSModel posModel = new POSModel(config.seedTerms.posModel.toFile());
        this.tagger = new ThreadLocal<POSTagger>() {
            @Override
            protected POSTagger initialValue() {
                return new POSTaggerME(posModel);
            }
        };
        
        if (config.seedTerms.lemmatizerModel == null) {
            this.lemmatizer = null;
        } else {
            final DictionaryLemmatizer dictLemmatizer = new DictionaryLemmatizer(config.seedTerms.lemmatizerModel.toFile());
            this.lemmatizer = new ThreadLocal<Lemmatizer>() {
                @Override
                protected Lemmatizer initialValue() {
                    return dictLemmatizer;
                }
            };
        }
	}
	
	public Result extractDomainModelTerms(Corpus corpus) {

		//1 - Extract seed terms and domain model candidates
		Result seedTerms = this.te.extractTerms(corpus);
		
		Result domainTermCandidates = this.dmCandidateExtraction.extractTerms(corpus);		
                       
        //2 - Extract cooccurrence stats between seed terms and domain model candidates
        Set<String> seedTermStrings = new HashSet<String> ();
        for(Term term: seedTerms.terms) {
        	seedTermStrings.add(term.getString());
        }
        
        Set<String> domainCandidateStrings = new HashSet<String> ();
        for(Term candidate: domainTermCandidates.terms) {
        	domainCandidateStrings.add(candidate.getString());
        }
        
        Map<String, Object2IntMap<String>> domainFreqs = new HashMap<>();
        
        extractCoocurrenceStats(corpus, 10, 
        		this.tokenizer, this.dmExtractionConfig.ngramMin, this.dmExtractionConfig.ngramMax, this.dmExtractionConfig.maxDocs, this.tagger, this.lemmatizer,
        		this.dmExtractionConfig.preceedingTokens, this.dmExtractionConfig.middleTokens, this.dmExtractionConfig.headTokens, this.dmExtractionConfig.headTokenFinal,
        		this.seedTermExtractionConfig.preceedingTokens, this.seedTermExtractionConfig.middleTokens, 
        		this.seedTermExtractionConfig.headTokens, this.seedTermExtractionConfig.headTokenFinal,
        		seedTermStrings, domainCandidateStrings, domainFreqs);
                
        //3 - Score each domain model term according to their stats (calculate PMI between seed terms and domain model terms)
        Object2DoubleMap<String> scores = calculateScores(seedTerms.terms, domainTermCandidates.terms, domainFreqs);
        
        //4 - Rank each domain model term (just a descent rank based on score value)
        
        List<String> domainModelTerms = new ArrayList<String>(scores.keySet());
        rankTerms(domainModelTerms, scores);
        
        //5 - Select top N according to this.maxTerms 
        
        if (domainModelTerms.size() > this.maxDomainModelTerms) {
        	domainModelTerms = domainModelTerms.subList(0, this.maxDomainModelTerms);
        }
        
        //6 - Return results
           
		return new Result(convertToTerms(domainTermCandidates.terms, domainModelTerms, scores), null, null);
	}
	
	/**
	 * Create {@link Term} objects from a list of strings and their scores
	 * 
	 * @param termStrings - the term strings
	 * @param scores - a map of term strings and their score
	 * 
	 * @return a {@link Set} of {@link Term}
	 */
	private static Set<Term> convertToTerms(Set<Term> domainModelTerms, List<String> termStrings, Object2DoubleMap<String> scores) {
        Set<Term> terms = new HashSet<>();
        for (Term candidate : domainModelTerms) {
        	if (termStrings.contains(candidate.getString())) {
        		candidate.setScore(scores.getDouble(candidate.getString()));
        		terms.add(candidate);
        	}
        }
        return terms;
    }
	
	private static boolean isProperTerm(String rootSequence, Set<String> stopWords) {
        String s = rootSequence;

        if (s.length() < 2) {
            return false;
        }
        // all words need to have at least 2 characters
        String[] words = s.split(" ");
        if (minimumWordLength(words) < 2) {
            return false;
        }

        if (s.contains("- ") || s.contains(" -")) {
            return false;
        }
        final char[] chars = s.toCharArray();

        // first character must be alphabetic
        if (!isAlpha(chars[0])) {
            return false;
        }
        if (!isAlphaNumeric(chars[chars.length - 1])) {
            return false;
        }

        // first or last word not in stopwords
        String firstWord = decapitalize(words[0]);
        String lastWord = decapitalize(words[words.length - 1]);
        if (stopWords.contains(firstWord) || stopWords.contains(lastWord)) {
            return false;
        }

        // is alpha numeric
        for (int x = 0; x < chars.length; x++) {
            final char c = chars[x];
            if (!isAlphaNumeric(c) && c != '-' && c != ' ') {
                return false;
            }
        }
        return true;
    }
	
	private static int minimumWordLength(String[] words) {
        int minLength = Integer.MAX_VALUE;

        for (String word : words) {
            if (minLength > word.length()) {
                minLength = word.length();
            }
        }
        return minLength;
    }

    private static boolean isAlpha(final char c) {
        return Character.isLetter(c);
    }

    private static boolean isAlphaNumeric(final char c) {
        return Character.isLetterOrDigit(c);
    }

    private static String decapitalize(String s) {
        if (s.length() <= 1) {
            return s.toLowerCase();
        } else {
            if (Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) {
                return s;
            } else {
                char[] c = s.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                return new String(c);
            }
        }
    }

    
    /**
     * Calculate PMI score between seed terms and domain model candidates 
     * 
     * @param seedTerms - a list of seed terms containing their string and their frequency
     * @param coocurrenceFreqs - co-ocurrence statistics between seed terms and domain model candidates
     *   key: seedTerm
     *   value:  Map(key: domainModelCandidateTerm, value: coocurrence with seedTerm)
     *   
     * @return a map (key: domainModelTerm, value: score)
     */
	private Object2DoubleMap<String> calculateScores(Set<Term> seedTerms, Set<Term> domainModelTerms, Map<String, Object2IntMap<String>> coocurrenceFreqs){
        
		Object2IntMap<String> seedTermFreq = new Object2IntLinkedOpenHashMap<>();
		for(Term seedTerm: seedTerms) {
			seedTermFreq.put(seedTerm.getString(), seedTerm.getOccurrences());
		}
        
        Object2IntMap<String> domainTermFreq = new Object2IntLinkedOpenHashMap<>();
        long N = 0;
        for(Term domainModelTerm: domainModelTerms) {
        	domainTermFreq.put(domainModelTerm.getString(), domainModelTerm.getOccurrences());
        	N+= domainModelTerm.getOccurrences();
		}

        Object2DoubleMap<String> pmis = new Object2DoubleOpenHashMap<>();
        for (Map.Entry<String, Object2IntMap<String>> DMFreqPerTermSeed : coocurrenceFreqs.entrySet()) {
            String seedTerm = DMFreqPerTermSeed.getKey();
            for (Object2IntMap.Entry<String> domainTermFreqForASeedTerm : DMFreqPerTermSeed.getValue().object2IntEntrySet()) {
                String domainModelTerm = domainTermFreqForASeedTerm.getKey();
                double ftw = (double) domainTermFreqForASeedTerm.getIntValue();
                double fw = (double) domainTermFreq.getInt(domainModelTerm);
                double ft = (double) seedTermFreq.getInt(seedTerm);
                double pmi = (Math.log(ftw / ft / fw) + Math.log(N));
                pmis.put(domainModelTerm, pmis.getDouble(domainModelTerm) + pmi);
            }
        }

        // We should divide by the size of the term set, however this is not 
        // necessary to sort
        return pmis;

    }

	/**
	 * Rank a list of terms based on their score
	 * 
	 * @param terms - the list of terms to be sorted
	 * @param scores - the score for each term
	 */
    private void rankTerms(List<String> terms, final Object2DoubleMap<String> scores) {

    	Set<String> stopWords = new HashSet<String>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS));
    	
        terms.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                boolean proper1 = isProperTerm(o1, stopWords);
                boolean proper2 = isProperTerm(o2, stopWords);
                if (proper1 == proper2) {
                    return -Double.compare(scores.getDouble(o1), scores.getDouble(o2));
                } else if (proper1) {
                    return -1;
                } else {
                    return +1;
                }
            }
        });
    }
	
    /**
     * 
     * Extract coocurrence statistics between seed terms and candidate domain model terms
     * 
     * @param corpus
     * @param nThreads
     * @param tokenizer
     * @param minLength
     * @param maxLength
     * @param maxDocs
     * @param tagger
     * @param lemmatizer
     * @param preceedingTokens
     * @param middleTokens
     * @param endTokens
     * @param headTokenFinal
     * @param preceedingDMTokens
     * @param middleDMTokens
     * @param endDMTokens
     * @param headDMTokenFinal
     * @param seedTerms
     * @param domainFreqs
     */
	private static void extractCoocurrenceStats(Corpus corpus, int nThreads,
            ThreadLocal<Tokenizer> tokenizer, int minLength, int maxLength, int maxDocs,
            ThreadLocal<POSTagger> tagger, ThreadLocal<Lemmatizer> lemmatizer, 
            Set<String> preceedingTokens, Set<String> middleTokens, Set<String> endTokens, boolean headTokenFinal, 
            Set<String> preceedingDMTokens, Set<String> middleDMTokens, Set<String> endDMTokens, boolean headDMTokenFinal, 
            Set<String> seedTerms, Set<String> dmCandidateTerms,
            Map<String, Object2IntMap<String>> domainFreqs) {
		
        ExecutorService service = new ThreadPoolExecutor(nThreads, nThreads, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());

        int docCount = 0;
        for (Document doc : corpus.getDocuments()) {
            service.submit(new TopWordsTask(doc, tokenizer, minLength, maxLength, seedTerms, dmCandidateTerms,
            		new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)), tagger, lemmatizer, 
            		preceedingTokens, middleTokens, endTokens, headTokenFinal,
            		domainFreqs));
            if (docCount++ > maxDocs) {
                break;
            }
        }

        service.shutdown();
        try {
            service.awaitTermination(2, TimeUnit.DAYS);
        } catch (InterruptedException x) {
            x.printStackTrace();
            throw new RuntimeException(x);
        }
    }
	
	private static class TopWordsTask implements Runnable {

        private final Document doc;
        private final ThreadLocal<Tokenizer> tokenizer;
        private final int minLength;
        private final int maxLength;
        private final Set<String> seedTerms;
        private final Set<String> candidateDomainModelTerms;
        private final ThreadLocal<POSTagger> tagger;
        private final ThreadLocal<Lemmatizer> lemmatizer;
        private final Set<String> preceedingTokens;
        private final Set<String> middleTokens;
        private final Set<String> endTokens;
        private final boolean headTokenFinal;

        private final Set<String> excludedTerms;
        private final int maxLengthSeedTerm;
        private final Set<String> stopWords;
        
        private final Map<String, Object2IntMap<String>> totalFreqs;
        

        public TopWordsTask(Document doc, ThreadLocal<Tokenizer> tokenizer, int minLength, int maxLength,
        		Set<String> seedTerms, Set<String> candidateDomainModelTerms, Set<String> excludedTerms,
        		ThreadLocal<POSTagger> tagger, ThreadLocal<Lemmatizer> lemmatizer,
        		Set<String> preceedingTokens, Set<String> middleTokens, Set<String> endTokens, boolean headTokenFinal,
        		Map<String, Object2IntMap<String>> totalFreqs) {
            this.doc = doc;
            this.tokenizer = tokenizer;
            this.minLength = minLength <=0 ? 1 : minLength;
            this.maxLength = maxLength;
            this.seedTerms = seedTerms;
            this.candidateDomainModelTerms = candidateDomainModelTerms;
            this.excludedTerms = excludedTerms;
            this.tagger = tagger;
            this.lemmatizer = lemmatizer;
            this.preceedingTokens = preceedingTokens;
            this.middleTokens = middleTokens;
            this.endTokens = endTokens;
            this.headTokenFinal = headTokenFinal;
            
            this.totalFreqs = totalFreqs;
            
            int maxLengthSeedTerm = 0;
    		for(String seedTerm: seedTerms) { 
    			if (maxLengthSeedTerm < seedTerm.length()) {
    				maxLengthSeedTerm = seedTerm.length();
    			}
    		}
    		this.maxLengthSeedTerm = maxLengthSeedTerm;
    		this.stopWords = new HashSet<String>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS));
        }

        @Override
        public void run() {
        	try {
	            String contents = doc.contents();
	            final Map<String, Object2IntMap<String>> jointFreq = new HashMap<>();
	            for (String sentence : contents.split("\n")) {
	                String[] tokens;
	                try {
	                    tokens = tokenizer.get().tokenize(sentence);
	                } catch (Exception x) {
	                    System.err.println(sentence);
	                    throw x;
	                }
	                if (tokens.length > 0) {
	                    final String[] tags = tagger.get().tag(tokens);

	                    if (tags.length != tokens.length) {
	                        throw new RuntimeException("Tagger did not return same number of tokens as tokenizer");
	                    }

	                    for (int i = 0; i < tokens.length; i++) {
	                        boolean nonStop = false;
	                        for (int j = i; j < min(i + this.maxLengthSeedTerm, tokens.length); j++) {
	                            if (!stopWords.contains(tokens[j].toLowerCase()) && !stopWords.contains(tokens[j])) {
	                                nonStop = true;
	                            }
	                            if (headTokenFinal) {
	                                if (endTokens.contains(tags[j]) && nonStop) {
	                                	generateTerm(jointFreq, tokens, tags, i, j);
	                                }
	                                if (!preceedingTokens.contains(tags[j]) && (i == j || !middleTokens.contains(tags[j]))) {
	                                    break;
	                                }
	                            } else {
	                                if (j == i && endTokens.contains(tags[j]) && nonStop) {
	                                	generateTerm(jointFreq, tokens, tags, i, j);
	                                }
	                                if (preceedingTokens.contains(tags[j]) && j != i) {
	                                	generateTerm(jointFreq, tokens, tags, i, j);
	                                }
	                                if (j == i && !endTokens.contains(tags[j])
	                                        || j > i && !middleTokens.contains(tags[j]) && !preceedingTokens.contains(tags[j])) {
	                                    break;
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	            
	            synchronized (totalFreqs) {
	                for (Map.Entry<String, Object2IntMap<String>> e2 : jointFreq.entrySet()) {
	                    if (!totalFreqs.containsKey(e2.getKey())) {
	                        totalFreqs.put(e2.getKey(), e2.getValue());
	                    } else {
	                        Object2IntMap<String> freq2 = totalFreqs.get(e2.getKey());
	                        for (Object2IntMap.Entry<String> e : e2.getValue().object2IntEntrySet()) {
	                            freq2.put(e.getKey(), freq2.getInt(e.getKey()) + e.getIntValue());
	                        }
	                    }
	                }
	            }
	        } catch (Exception x) {
	            x.printStackTrace();
	        }
        }

		private void generateTerm(final Map<String, Object2IntMap<String>> jointFreq, String[] tokens,
				final String[] tags, int i, int j) {
			String seedTerm = emitTerm(j, i, tokens, tags, headTokenFinal);
			
			if (seedTerms.contains(seedTerm)) {
				
			    if (!jointFreq.containsKey(seedTerm)) {
			        jointFreq.put(seedTerm, new Object2IntOpenHashMap<String>());
			    }
			    Object2IntMap<String> freq2 = jointFreq.get(seedTerm);
			    
				List<String> dmCandidates = extractDomainModelTermsFromContextWindow(
						IntStream.range(Math.max(0, i-this.maxLength),i).mapToObj(e -> tokens[e]).toArray(String[]::new),
						IntStream.range(Math.max(0, i-this.maxLength),i).mapToObj(e -> tags[e]).toArray(String[]::new)
				);

			    for(String dmCandidate: dmCandidates) {
			    	freq2.put(dmCandidate, freq2.getInt(dmCandidate) +1);
			    }
			}
		}
        
		/**
		 * Generate a string for a term given a set of tokens and tags
		 * 
		 * @return a term string
		 */
        private String emitTerm(int j, int i, String[] tokens, String[] tags,
	            boolean headTokenFinal) {
	        if (lemmatizer != null && lemmatizer.get() != null ) {//&& j - i + 1 >= ngramMin) { //TODO: Check if it this is required
	            String[] ltoks = Arrays.copyOf(tokens, tokens.length);
	            for (int n = 0; n < ltoks.length; n++) {
	                ltoks[n] = ltoks[n].toLowerCase();
	            }
	            String[] lemmas = lemmatizer.get().lemmatize(ltoks, tags);
	            String[] tokens2 = Arrays.copyOfRange(tokens, i, j + 1);
	            if (headTokenFinal) {
	                if (!lemmas[j].equals("O") && !lemmas[j].equalsIgnoreCase("datum")) {
	                    tokens2[tokens2.length - 1] = lemmas[j];
	                }
	            } else {
	                if (!lemmas[i].equals("O")) {
	                    tokens2[0] = lemmas[i];
	                }
	            }
	            return processTerm(tokens2, 0, j - i);
	        } else {
	            return processTerm(tokens, i, j);
	        }
	    }
        
        private String processTerm(String[] tokens, int i, int j) {
            String termStrOrig = join(tokens, i, j+1);
            String termStr = termStrOrig.toLowerCase();
            return termStr;
	    }
        
        /**
         * Extract a set of valid domain model terms from a context window.
         * 
         * @param tokens - the tokens in the context window
         * @param tags - the part-of-speech tags in the context window
         * 
         * @return A list of valid terms appearing in the context window (allows term overlapping)
         */
        private List<String> extractDomainModelTermsFromContextWindow(String[] tokens, String[] tags) {
        	List<String> extractedTerms = new ArrayList<String>();
        	
        	for(int i=0; i <= tokens.length - this.maxLength; i++) {
        		for(int j=i+this.minLength; j<= tokens.length; j++) {
        			String term = join(tokens, i, j);
        			if (!excludedTerms.contains(term) && this.candidateDomainModelTerms.contains(term)) {
        				extractedTerms.add(term);
        			}
        		}
        	}
        	
        	return extractedTerms;
        }
        
        private static String join(String[] tokens, int i, int j) {
            StringBuilder term = new StringBuilder();
            for (int x = i; x < j; x++) {
                if (x != i) {
                    term.append(" ");
                }
                term.append(tokens[x]);
            }
            return term.toString();
        }
    }
	
	private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
	
	public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("x", "The corpus index to read").withRequiredArg().ofType(File.class);
                    accepts("t", "The domain model terms to write").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            ObjectMapper mapper = new ObjectMapper();

            if (os.valueOf("c") == null) {
                badOptions(p, "Configuration is required");
                return;
            }
            if (os.valueOf("x") == null) {
                badOptions(p, "Corpus is required");
                return;
            }
            if (os.valueOf("t") == null) {
                badOptions(p, "Output for Terms is required");
                return;
            }
            DomainModelExtractionConfiguration c = null;
            
            try {
            	Configuration config = mapper.readValue((File) os.valueOf("c"), Configuration.class);
            	c = config.dmExtraction;
            } catch (Exception e) {
            	c = mapper.readValue((File) os.valueOf("c"), DomainModelExtractionConfiguration.class);
            }
            
            File corpusFile = (File) os.valueOf("x");
            final Corpus corpus = mapper.readValue(corpusFile, CollectionCorpus.class);

            final DomainTermExtraction extractor = new DomainTermExtraction(c);

            final Result r = extractor.extractDomainModelTerms(corpus);
            r.normalize();

            mapper.writerWithDefaultPrettyPrinter().writeValue((File) os.valueOf("t"), r.terms);

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }

    }
}
