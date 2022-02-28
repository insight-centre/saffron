package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.VirtualRootTaxonomy;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A (partial) solution to the taxonomy search problem
 *
 * @author John McCrae
 */
public class TaxonomySolution extends Solution{



    public final Map<String, Taxonomy> heads;
    public final Set<String> terms;
    public final int size;

    /**
     * Create a new solution
     *
     * @param heads The heads of the taxonomy fragments
     * @param terms The set of terms required to be in the solution
     */
    public TaxonomySolution(Map<String, Taxonomy> heads, Set<String> terms) {
        this.heads = heads;
        this.terms = terms;
        this.size = calcSize();
    }

    private TaxonomySolution(Map<String, Taxonomy> heads, Set<String> terms, int size) {
        this.heads = heads;
        this.terms = terms;
        this.size = size;
    }

    /**
     * Create a new empty solution
     *
     * @param terms The terms included in the complete solution
     * @return An empty solution
     */
    public static TaxonomySolution empty(Set<String> terms) {
        return new TaxonomySolution(new HashMap<String, Taxonomy>(), terms);
    }

    /**
     * Add a link to create a new partial solution
     *
     * @param top The top (broader) term
     * @param bottom The bottom (narrower) term
     * @param topScore The score of the top term
     * @param bottomScore The score of the bottom term
     * @param linkScore The link score
     * @param accepted Is this an accepted (whitelisted) term
     * @return
     */
    public TaxonomySolution add(final String top, final String bottom,
                                final double topScore, final double bottomScore,
                                final double linkScore,
                                final boolean accepted) {

        if (heads.containsKey(bottom)) {

            for (Map.Entry<String, Taxonomy> e : heads.entrySet()) {
                if (e.getValue().hasDescendent(top)) {
                    if (e.getKey().equals(bottom)) {
                        return null;
                    }
                    // Connecting bottom to an existing top
                    Map<String, Taxonomy> newHeads = new HashMap<>(heads);
                    newHeads.remove(bottom);
                    newHeads.put(e.getKey(),
                            insertIntoTaxo(newHeads.get(e.getKey()), top, heads.get(bottom).withLinkScore(linkScore).deepCopy()));
                    return new TaxonomySolution(newHeads, terms, size);
                }
            }
            // top is not yet in taxonomy
            Map<String, Taxonomy> newHeads = new HashMap<>(heads);
            Taxonomy t2 = new Taxonomy(top, topScore, Double.NaN, new ArrayList<>(Arrays.asList(newHeads.get(bottom).withLinkScore(linkScore))), accepted ? Status.accepted : Status.none);
            newHeads.remove(bottom);
            newHeads.put(top, t2);
            return new TaxonomySolution(newHeads, terms, size + 1);
        } else {
            for (Map.Entry<String, Taxonomy> e : heads.entrySet()) {
                if (e.getValue().hasDescendent(bottom)) {
                    return null;
                }
            }
            // bottom is not yet in taxonomy
            for (Map.Entry<String, Taxonomy> e : heads.entrySet()) {
                if (e.getValue().hasDescendent(top)) {
                    // But top is
                    Map<String, Taxonomy> newHeads = new HashMap<>(heads);
                    newHeads.put(e.getKey(),
                            insertIntoTaxo(newHeads.get(e.getKey()), top,
                                    new Taxonomy(bottom, bottomScore, linkScore, new ArrayList<Taxonomy>(), accepted ? Status.accepted : Status.none)));
                    return new TaxonomySolution(newHeads, terms, size + 1);
                }
            }
            // top and bottom are not in the taxonomy
            Map<String, Taxonomy> newHeads = new HashMap<>(heads);
            Taxonomy t = new Taxonomy(top, topScore, Double.NaN, new ArrayList<Taxonomy>() {
                {
                    add(new Taxonomy(bottom, bottomScore, linkScore, new ArrayList<Taxonomy>(), accepted ? Status.accepted : Status.none));
                }
            }, Status.none);
            newHeads.put(top, t);
            return new TaxonomySolution(newHeads, terms, size + 2);
        }
    }

    /**
     * *
     * Check if the solution has completed
     *
     * @return true if the solution is valid
     */
    public boolean isComplete() {
        return size() == terms.size();
    }

    /**
     * Convert this to a taxonomy (if it complete)
     *
     * @return The complete taxonomy
     * @throws IllegalStateException If the solution is not complete
     */
    public Taxonomy toTaxonomy() {
        if (isComplete()) {
            if (heads.size() > 1)
                return new VirtualRootTaxonomy(heads.values());
            else
                return heads.values().iterator().next();
        } else {
            throw new IllegalStateException("Cannot convert to a taxonomy until this taxonomy is complete");
        }
    }

    private Taxonomy insertIntoTaxo(Taxonomy taxo, String top, Taxonomy bottom) {
        ArrayList<Taxonomy> newChildren = new ArrayList<>();
        if (taxo.root.equals(top)) {
            for (Taxonomy t : taxo.children) {
                newChildren.add(t);
            }
            newChildren.add(bottom);
        } else {
            for (Taxonomy t : taxo.children) {
                newChildren.add(insertIntoTaxo(t, top, bottom));
            }
        }
        return new Taxonomy(taxo.root, taxo.score, taxo.linkScore, newChildren, taxo.status);
    }

    /**
     * Return an indicator of the completeness of the solution
     *
     * @return The completeness of the solution
     */
    public int size() {
        return size;
    }

    private int calcSize() {
        int size = 0;
        for (Taxonomy t : heads.values()) {
            size += t.size();
        }
        return size;
    }

    @Override
    public String toString() {
        return "Solution{" + "heads=" + heads + ", terms=" + terms + ", size=" + size + '}';
    }
}
