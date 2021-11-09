package org.insightcentre.nlp.saffron.term;

import static java.lang.Integer.min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.term.enrich.EnrichTerms.WordTrie;

import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TermExtractionTask implements Runnable {

    private final Document doc;
    private final ThreadLocal<POSTagger> tagger;
    private final ThreadLocal<Lemmatizer> lemmatizer;
    private final ThreadLocal<Tokenizer> tokenizer;
    private final Set<String> stopWords;
    private final int ngramMin;
    private final int ngramMax;
    private final FrequencyStats stats = new FrequencyStats();
    private final Set<String> preceedingTokens;
    private final Set<String> middleTokens;
    private final Set<String> endTokens;
    private final boolean headTokenFinal;
    private final Set<String> blacklist;
    private final WordTrie domainModel;

    //Outputs to be calculated
    private final FrequencyStats summary;
    private final CasingStats casing;
    private final TemporalFrequencyStats temporalFrequency;
    private final RelationshipStats relStats;
    private final ConcurrentLinkedQueue<DocumentTerm> docTerms;

    public TermExtractionTask(Document doc, ThreadLocal<POSTagger> tagger,
            ThreadLocal<Lemmatizer> lemmatizer,
            ThreadLocal<Tokenizer> tokenizer,
            Set<String> stopWords, int ngramMin, int ngramMax,
            Set<String> preceedingTokens, Set<String> middleTokens,
            Set<String> endTokens,
            boolean headTokenFinal,
            FrequencyStats summary,
            ConcurrentLinkedQueue<DocumentTerm> docTerms,
            CasingStats casing,
            Set<String> blacklist,
            TemporalFrequencyStats temporalFrequency,
            List<String> domainModel, RelationshipStats relStats) {
        this.doc = doc;
        this.tagger = tagger;
        this.lemmatizer = lemmatizer;
        this.tokenizer = tokenizer;
        this.stopWords = stopWords;
        this.ngramMin = ngramMin;
        this.ngramMax = ngramMax;
        this.preceedingTokens = preceedingTokens;
        this.middleTokens = middleTokens;
        this.endTokens = endTokens;
        this.summary = summary;
        this.headTokenFinal = headTokenFinal;
        this.docTerms = docTerms;
        this.casing = casing;
        this.blacklist = blacklist;
        this.temporalFrequency = temporalFrequency;
        if (domainModel == null || domainModel.size() == 0) {
        	this.domainModel =  null;
        } else {
        	Tokenizer tok = tokenizer.get();
            WordTrie trie = new WordTrie("");
            for (String domainModelTerm : domainModel) {
                trie.addTokenized(tok.tokenize(domainModelTerm.toLowerCase()));
            }
            this.domainModel = trie;
        }
        this.relStats = relStats;
    }

    @Override
    public void run() {
        try {
            final HashMap<String, DocumentTerm> docTermMap = docTerms != null
                    ? new HashMap<String, DocumentTerm>()
                    : null;
            String contents = doc.contents();
            CasingStats localCasing = new CasingStats();
            RelationshipStats localRelStats = new RelationshipStats();
            for (String sentence : contents.split("\n")) {
                String[] tokens;
                try {
                    tokens = tokenizer.get().tokenize(sentence);
                } catch (Exception x) {
                    System.err.println(sentence);
                    throw x;
                }
                if (tokens.length > 0) {
                    String[] tags = new String[0];
                    tags = tagger.get().tag(tokens);

                    if (tags.length != tokens.length) {
                        throw new RuntimeException("Tagger did not return same number of tokens as tokenizer");
                    }

                    List<WordTrie> dmPartials = new ArrayList<WordTrie>();
                    for (int i = 0; i < tokens.length; i++) {
                        boolean nonStop = false;


                        //Loop here until matching a domain model word, when it does, then let it into the next loop with j = i+1
                        List<String> activatedDMTerms = null;
                        if (domainModel != null) {
                        	/*
                        	 * 1 - For each dmPartial:
                        	 *  1.1 - if the current token makes it a domain model term, then change the flag
                        	 *  1.2 - if the current token makes it not a prefix to any domain model term, then discard the partial
                        	 * 2 - If the current token alone is a prefix, then add it to the partials
                        	 * 3 - If the current token alone is a domain model term, then change the flag
                        	 *
                        	 * 4 - if flag is false, then do not perform term extraction
                        	 */

                        	activatedDMTerms = new ArrayList<String>();

                        	List<WordTrie> updatedPartials = new ArrayList<WordTrie>();
                        	for(WordTrie dmPartial: dmPartials) {
                        		WordTrie partial = dmPartial.get(tokens[i]);
                        		if(partial != null) {
                        			if (partial.isPresent()) {
                        				activatedDMTerms.add(partial.getWord());
                        			}

                        			if(partial.isPrefix()) {
                        				updatedPartials.add(partial);
                        			}
                        		}
                        	}
                        	WordTrie partial = this.domainModel.get(tokens[i]);
                        	if(partial != null) {
                        		if (partial.isPresent()) {
                        			activatedDMTerms.add(tokens[i]);
                    			}

                    			if(partial.isPrefix()) {
                    				updatedPartials.add(partial);
                    			}
                        	}

                        	dmPartials = updatedPartials;

                        	if(activatedDMTerms.size() == 0) {
                        		continue;
                        	}
                        }

                        int startIndex = 0;
                        if (domainModel == null)
                        	startIndex = i;
                        else
                        	startIndex = i+1;

                        for (int j = startIndex; j < min(startIndex + ngramMax, tokens.length); j++) {
                            if (!stopWords.contains(tokens[j].toLowerCase()) && !stopWords.contains(tokens[j])) {
                                nonStop = true;
                            }

	                            if (headTokenFinal) {
	                                if (endTokens.contains(tags[j]) && nonStop) {
	                                    String term = emitTerm(j, startIndex, tokens, tags, docTermMap, localCasing, headTokenFinal);
	                                    connectDomainModelAndTerm(localRelStats, activatedDMTerms, term);
	                                }
	                                if (!preceedingTokens.contains(tags[j]) && (startIndex == j || !middleTokens.contains(tags[j]))) {
	                                    break;
	                                }
	                            } else {
	                                if (j == startIndex && endTokens.contains(tags[j]) && nonStop) {
	                                	String term = emitTerm(j, startIndex, tokens, tags, docTermMap, localCasing, headTokenFinal);
	                                    connectDomainModelAndTerm(localRelStats, activatedDMTerms, term);
	                                }
	                                if (preceedingTokens.contains(tags[j]) && j != startIndex) {
	                                	String term = emitTerm(j, startIndex, tokens, tags, docTermMap, localCasing, headTokenFinal);
	                                    connectDomainModelAndTerm(localRelStats, activatedDMTerms, term);
	                                }
	                                if (j == startIndex && !endTokens.contains(tags[j])
	                                        || j > startIndex && !middleTokens.contains(tags[j]) && !preceedingTokens.contains(tags[j])) {
	                                    break;
	                                }
	                            }
                        }
                    }
                }
                stats.tokens += tokens.length;
            }

            stats.documents = 1;

            synchronized (summary) {
                summary.add(stats);
                if(doc.date != null && temporalFrequency != null)
                    temporalFrequency.add(stats, doc.date);
            }
            if (casing != null) {
                synchronized (casing) {
                    casing.add(localCasing);
                }
            }
            if (docTermMap != null) {
                docTerms.addAll(docTermMap.values());
            }
            if (relStats!= null && !localRelStats.isEmpty()) {
            	synchronized (relStats) {
					relStats.add(localRelStats);
				}
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

	private void connectDomainModelAndTerm(RelationshipStats localRelStats, List<String> activatedDMTerms,
			String term) {
		if (activatedDMTerms != null && term!=null) {
		    for(String dmTerm: activatedDMTerms) {
		    	localRelStats.addRelation(dmTerm, term);
		    }
		}
	}

    private String emitTerm(int j, int i, String[] tokens, String[] tags,
            final HashMap<String, DocumentTerm> docTermMap, CasingStats localCasing,
            boolean headTokenFinal) {
        if (lemmatizer != null && lemmatizer.get() != null && j - i + 1 >= ngramMin) {
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
            return processTerm(tokens2, 0, j - i, docTermMap,
                    localCasing);
        } else {
            return processTerm(tokens, i, j, docTermMap,
                    localCasing);
        }
    }

    public static String join(String[] tokens, int i, int j) {
        StringBuilder term = new StringBuilder();
        for (int x = i; x <= j; x++) {
            if (x != i) {
                term.append(" ");
            }
            term.append(tokens[x]);
        }
        return term.toString();

    }

    private boolean isValidTerm(String term) {
        return !blacklist.contains(term.toLowerCase()) && term.matches(".*\\p{Alpha}.*")
                && (term.matches("\\p{Alpha}.*") || term.matches(".*\\p{Alpha}"))
                && term.length() > 1
                && !term.startsWith("http://") && !term.startsWith("https://");
    }

    private String  processTerm(String[] tokens, int i, int j,
            HashMap<String, DocumentTerm> dts,
            CasingStats localCasing) {
        if (j - i >= this.ngramMin - 1) {
            boolean allValid = true;
            for (int k = i; k <= j; k++) {
                allValid = allValid && isValidTerm(tokens[k]);
            }
            String termStrOrig = join(tokens, i, j);
            String termStr = termStrOrig.toLowerCase();
            if (allValid && termStr.length() > 2) {
                stats.docFrequency.put(termStr, 1);
                stats.termFrequency.put(termStr, 1 + stats.termFrequency.getInt(termStr));
                if (dts != null) {
                    dts.put(termStr, new DocumentTerm(doc.id, termStr,
                            stats.termFrequency.getInt(termStr), null, null, null));
                }
                if (j - i == 0) {
                    localCasing.addCasing(termStrOrig);
                }
                return termStr;
            }
        }
        return null;
    }

}
