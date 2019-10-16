package org.insightcentre.nlp.saffron.taxonomy.supervised;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TaxonomyWithSize;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 * Calculcate the sizes of the taxonomy
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class AddSizesToTaxonomy {
    
    public static TaxonomyWithSize addSizes(Taxonomy taxonomy, List<DocumentTopic> docTopics) {
        Object2IntOpenHashMap<String> topicFreq = new Object2IntOpenHashMap<>();
        for(DocumentTopic dt : docTopics) {
            topicFreq.put(dt.getTermString(), dt.getOccurrences());
        }
        return _addSizes(taxonomy, topicFreq, 0);
    }

    private static TaxonomyWithSize _addSizes(Taxonomy taxonomy, Object2IntOpenHashMap<String> topicFreq, int total) {
        ArrayList<TaxonomyWithSize> children = new ArrayList<>();
        int size = topicFreq.getInt(taxonomy.root) + total;
        for(Taxonomy t : taxonomy.children) {
            children.add(_addSizes(t, topicFreq, size));
        }
        return new TaxonomyWithSize(taxonomy.root, taxonomy.score, children, topicFreq.getInt(taxonomy.root));
    }
    
}
