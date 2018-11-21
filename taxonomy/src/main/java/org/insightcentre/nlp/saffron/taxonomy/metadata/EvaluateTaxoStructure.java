package org.insightcentre.nlp.saffron.taxonomy.metadata;

import java.io.File;
import java.io.IOException;

import org.insightcentre.nlp.saffron.data.Taxonomy;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Collect stats about the structure of a taxonomy
 * 
 * @author biaper
 */
public class EvaluateTaxoStructure {
	
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
                    accepts("t", "The taxonomy to use").withRequiredArg().ofType(File.class);
                    accepts("o", "Where to write the output statistics").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            
            final File taxonomyFile = (File) os.valueOf("t");
            if (taxonomyFile == null || !taxonomyFile.exists()) {
                badOptions(p, "Taxonomy file does not exist");
            }
            final File output = (File) os.valueOf("o");
            if (output == null) {
                badOptions(p, "Output not specified");
            }
            
            evaluate(taxonomyFile, output);
            
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
	}
	
	private static void evaluate(File taxonomyFile, File outputFile) throws JsonParseException, JsonMappingException, IOException {
		
		final Taxonomy taxo = Taxonomy.fromJsonFile(taxonomyFile);

		TaxonomyStats stats = new TaxonomyStats(taxo);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile,stats);
	}
	
	private static class TaxonomyStats {
		
		private int numberOfNodes;
		private float minDegree;
		private double avgDegree;
		private float maxDegree;
		private int minDepth;
		private double medianDepth;
		private int maxDepth;
	    
		public TaxonomyStats() {
			
		}
		
	    public TaxonomyStats(Taxonomy taxo) {
	    	numberOfNodes = taxo.size();
	        minDegree = 1;// Taxonomies are trees and their leaves always have degree=1
	        avgDegree = taxo.avgDegree();
	        maxDegree = taxo.maxDegree();
	        minDepth = taxo.minDepth();
	        medianDepth = taxo.medianDepth();
	        maxDepth = taxo.depth();
	    }

		public int getNumberOfNodes() {
			return numberOfNodes;
		}

		public void setNumberOfNodes(int numberOfNodes) {
			this.numberOfNodes = numberOfNodes;
		}

		public float getMinDegree() {
			return minDegree;
		}

		public void setMinDegree(float minDegree) {
			this.minDegree = minDegree;
		}

		public double getAvgDegree() {
			return avgDegree;
		}

		public void setAvgDegree(double avgDegree) {
			this.avgDegree = avgDegree;
		}

		public float getMaxDegree() {
			return maxDegree;
		}

		public void setMaxDegree(float maxDegree) {
			this.maxDegree = maxDegree;
		}

		public int getMinDepth() {
			return minDepth;
		}

		public void setMinDepth(int minDepth) {
			this.minDepth = minDepth;
		}

		public double getMedianDepth() {
			return medianDepth;
		}

		public void setMedianDepth(double medianDepth) {
			this.medianDepth = medianDepth;
		}

		public int getMaxDepth() {
			return maxDepth;
		}

		public void setMaxDepth(int maxDepth) {
			this.maxDepth = maxDepth;
		}	    
	    
	}
}


