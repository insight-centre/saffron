package org.insightcentre.nlp.saffron.taxonomy.search;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

/**
 *
 * @author John McCrae
 */
public class SumScore implements TaxonomyScore {
    private final SupervisedTaxo classifier;
    private final Object2DoubleMap<TaxoLink> scores = new Object2DoubleOpenHashMap<>();

    public SumScore(SupervisedTaxo classifier) {
        this.classifier = classifier;
    }
    
    @Override
    public TaxonomyLinkScore score(Solution solution) {
        return new SumLinkScore();
    }
    
    private class SumLinkScore implements TaxonomyLinkScore {

        @Override
        public double deltaScore(TaxoLink tl) {
            if(!scores.containsKey(tl))
                scores.put(tl, classifier.predict(tl.top, tl.bottom));
            return scores.getDouble(tl);
        }
        
    }
    
    

}
