package org.insightcentre.nlp.saffron.config;

/**
 * Configuration for the search in the taxonomy algorithm
 * 
 * @author John McCrae
 */
public class TaxonomySearchConfiguration {
    /**
     * The algorithm to use for finding a taxonomy
     */
    public Algorithm algorithm = Algorithm.greedy;
    /**
     * (Beam search only) The size of the beam to use in the beam search
     */
    public int beamSize = 20;
    /**
     * The scoring function to optimize
     */
    public Score score = Score.simple;
    /**
     * (Bhattacharrya-Poisson only) The base metric for BP
     */
    public Score baseScore = Score.simple;
    /**
     * (Bhattacharrya-Poisson only) The average number of children
     */
    public double aveChildren = 3;
    /**
     * (Bhattacharrya-Poisson only) The weighting to give to BP (against the base algorithm)
     */
    public double alpha = 1.0;
    
    public enum Algorithm { greedy, beam };
    
    public enum Score { simple, transitive, bhattacharryaPoisson };

    @Override
    public String toString() {
        return "TaxonomySearchConfiguration{" + "algorithm=" + algorithm + ", beamSize=" + beamSize + ", score=" + score + ", baseScore=" + baseScore + ", aveChildren=" + aveChildren + ", alpha=" + alpha + '}';
    }
 
    
}
