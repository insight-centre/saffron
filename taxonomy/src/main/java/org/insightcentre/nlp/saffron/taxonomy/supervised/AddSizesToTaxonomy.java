package org.insightcentre.nlp.saffron.taxonomy.supervised;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TaxonomyWithSize;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;

/**
 * Calculcate the sizes of the taxonomy
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class AddSizesToTaxonomy {
    
    public static TaxonomyWithSize addSizes(Taxonomy taxonomy, List<DocumentTerm> docTerms) {
        Object2IntOpenHashMap<String> termFreq = new Object2IntOpenHashMap<>();
        for(DocumentTerm dt : docTerms) {
            termFreq.put(dt.getTermString(), dt.getOccurrences());
        }
        return _addSizes(taxonomy, termFreq, 0);
    }

    private static TaxonomyWithSize _addSizes(Taxonomy taxonomy, Object2IntOpenHashMap<String> termFreq, int total) {
        ArrayList<TaxonomyWithSize> children = new ArrayList<>();
        int size = termFreq.getInt(taxonomy.root) + total;
        for(Taxonomy t : taxonomy.children) {
            children.add(_addSizes(t, termFreq, size));
        }
        return new TaxonomyWithSize(taxonomy.root, taxonomy.score, children, termFreq.getInt(taxonomy.root));
    }
    
}
