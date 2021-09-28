package org.insightcentre.nlp.saffron.term.tools;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.CollectionCorpus;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.term.FrequencyStats;
import org.insightcentre.nlp.saffron.term.TermExtraction;

import com.fasterxml.jackson.databind.ObjectMapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Converts a Saffron corpus into reference corpus to be used with 'Weirdness' and 'Relevance'
 * term extraction metrics
 * 
 * @author Bianca Pereira
 *
 */
public class GenerateReferenceCorpus {


	 private static void badOptions(OptionParser p, String message) throws IOException {
	        System.err.println("Error: " + message);
	        p.printHelpOn(System.err);
	        System.exit(-1);
	    }
	
	public static void main(String[] args) {
		
		try { 
			final OptionParser p = new OptionParser() {
	            {
	                accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
	                accepts("x", "The corpus to read (in Saffron format)").withRequiredArg().ofType(File.class);
	                accepts("o", "The output file with the reference corpus").withRequiredArg().ofType(File.class);
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
	        if (os.valueOf("o") == null) {
	            badOptions(p, "Output file is required");
	            return;
	        }
	        
	    	/*
	    	 * 1 - Read Saffron corpus
	    	 * 2 - Perform Term Extraction
	    	 * 3 - Get only the FrequencyStats
	    	 * 4 - Print it to a JSON file
	    	 */
	        
	        TermExtractionConfiguration config = mapper.readValue((File) os.valueOf("c"), Configuration.class).termExtraction;
			
	        File corpusFile = (File) os.valueOf("x");
	        final Corpus corpus = mapper.readValue(corpusFile, CollectionCorpus.class);
	        
	        TermExtraction extractor = new TermExtraction(config);
	        FrequencyStats stats = extractor.extractStats(corpus, null, null, Collections.EMPTY_SET).frequencyStats;
	        
	        mapper.writerWithDefaultPrettyPrinter().writeValue((File) os.valueOf("o"), stats);
	        
	        
		} catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
	}
}
