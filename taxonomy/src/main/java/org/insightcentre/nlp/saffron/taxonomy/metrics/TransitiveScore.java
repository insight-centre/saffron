package org.insightcentre.nlp.saffron.taxonomy.metrics;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxoLink;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

/**
 *
 * @author John McCrae
 */
public class TransitiveScore implements TaxonomyScore {

    private final SupervisedTaxo classifier;
    private final Object2DoubleMap<TaxoLink> scores;
    final HashMap<String, Set<String>> parents;
    final HashMap<String, Set<String>> children;

    public TransitiveScore(SupervisedTaxo classifier) {
        this.classifier = classifier;
        this.parents = new HashMap<>();
        this.children = new HashMap<>();
        this.scores = new Object2DoubleOpenHashMap<>();
    }

    public TransitiveScore(SupervisedTaxo classifier, Object2DoubleMap<TaxoLink> scores, HashMap<String, Set<String>> parents, HashMap<String, Set<String>> children) {
        this.classifier = classifier;
        this.scores = scores;
        this.parents = parents;
        this.children = children;
    }

    @Override
    public double deltaScore(TaxoLink tl) {
        if (!scores.containsKey(tl)) {
            scores.put(tl, classifier.predict(tl.getTop(), tl.getBottom()) - 0.5);
        }
        double s = scores.getDouble(tl);
        if (parents.containsKey(tl.getTop())) {
            for (String p : parents.get(tl.getTop())) {
                TaxoLink tl2 = new TaxoLink(p, tl.getBottom());
                if (!scores.containsKey(tl2)) {
                    scores.put(tl2, classifier.predict(tl2.getTop(), tl2.getBottom()) - 0.5);
                }
                s += scores.getDouble(tl2);
                if (children.containsKey(tl.getBottom())) {
                    for (String c : children.get(tl.getBottom())) {
                        TaxoLink tl3 = new TaxoLink(p, c);
                        if (!scores.containsKey(tl3)) {
                            scores.put(tl3, classifier.predict(tl3.getTop(), tl3.getBottom()) - 0.5);
                        }
                        s += scores.getDouble(tl3);

                    }
                }
            }
        }
        if (children.containsKey(tl.getBottom())) {
            for (String c : children.get(tl.getBottom())) {
                TaxoLink tl2 = new TaxoLink(tl.getTop(), c);
                if (!scores.containsKey(tl2)) {
                    scores.put(tl2, classifier.predict(tl2.getTop(), tl2.getBottom()) - 0.5);
                }
                s += scores.getDouble(tl2);
            }
        }
        return s;
    }

    @Override
    public TaxonomyScore next(TaxoLink link, Solution soln) {
        HashMap<String, Set<String>> newParents = new HashMap<>(parents);
        // Shouldn't already be parents
        Set<String> p = newParents.containsKey(link.getBottom()) ? newParents.get(link.getBottom()) : new HashSet<String>();

        HashMap<String, Set<String>> newChildren = new HashMap<>(children);
        Set<String> c = newChildren.containsKey(link.getTop()) ? newChildren.get(link.getTop()) : new HashSet<String>();

        if (newParents.containsKey(link.getTop())) {
            p.addAll(newParents.get(link.getTop()));
        }

        if (newChildren.containsKey(link.getBottom())) {
            c.addAll(newChildren.get(link.getBottom()));
        }

        for (String parent : p) {
            if (!newChildren.containsKey(parent)) {
                newChildren.put(parent, new HashSet<String>());
            }
            newChildren.get(parent).addAll(c);
            newChildren.get(parent).add(link.getBottom());
        }

        for (String child : c) {
            if (!newParents.containsKey(child)) {
                newParents.put(child, new HashSet<String>());
            }
            newParents.get(child).addAll(p);
            newParents.get(child).add(link.getTop());
        }

        p.add(link.getTop());
        newParents.put(link.getBottom(), p);

        c.add(link.getBottom());
        newChildren.put(link.getTop(), c);

        return new TransitiveScore(classifier, scores, newParents, newChildren);
    }

}
