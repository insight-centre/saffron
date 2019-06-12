package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Taxonomy;

/**
 * A (partial) solution to the taxonomy search problem
 *
 * @author John McCrae
 */
public class Solution {

    public final Map<String, Taxonomy> heads;
    public final Set<String> topics;
    public final int size;

    /**
     * Create a new solution
     *
     * @param heads The heads of the taxonomy fragments
     * @param topics The set of topics required to be in the solution
     */
    public Solution(Map<String, Taxonomy> heads, Set<String> topics) {
        this.heads = heads;
        this.topics = topics;
        this.size = calcSize();
    }

    private Solution(Map<String, Taxonomy> heads, Set<String> topics, int size) {
        this.heads = heads;
        this.topics = topics;
        this.size = size;
    }

    /**
     * Create a new empty solution
     *
     * @param topics The topics included in the complete solution
     * @return An empty solution
     */
    public static Solution empty(Set<String> topics) {
        return new Solution(new HashMap<String, Taxonomy>(), topics);
    }

    /**
     * Add a link to create a new partial solution
     *
     * @param top The top (broader) topic
     * @param bottom The bottom (narrower) topic
     * @param topScore The score of the top topic
     * @param bottomScore The score of the bottom topic
     * @param linkScore The link score
     * @return
     */
    public Solution add(final String top, final String bottom,
            final double topScore, final double bottomScore, final double linkScore) {
        if (heads.containsKey(bottom)) {
            if(Double.isNaN(heads.get(bottom).linkScore)) {
                heads.put(bottom, heads.get(bottom).withLinkScore(linkScore));
            } else {
                assert(heads.get(bottom).linkScore == linkScore);
            }
            for (Map.Entry<String, Taxonomy> e : heads.entrySet()) {
                if (e.getValue().hasDescendent(top)) {
                    if (e.getKey().equals(bottom)) {
                        return null;
                    }
                    // Connecting bottom to an existing top
                    Map<String, Taxonomy> newHeads = new HashMap<>(heads);
                    newHeads.remove(bottom);
                    newHeads.put(e.getKey(),
                            insertIntoTaxo(newHeads.get(e.getKey()), top, heads.get(bottom).deepCopy()));
                    return new Solution(newHeads, topics, size);
                }
            }
            // top is not yet in taxonomy
            Map<String, Taxonomy> newHeads = new HashMap<>(heads);
            Taxonomy t2 = new Taxonomy(top, topScore, Double.NaN, new ArrayList<>(Arrays.asList(newHeads.get(bottom))), "none");
            newHeads.remove(bottom);
            newHeads.put(top, t2);
            return new Solution(newHeads, topics, size + 1);
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
                                    new Taxonomy(bottom, bottomScore, linkScore, new ArrayList<Taxonomy>(), "none")));
                    return new Solution(newHeads, topics, size + 1);
                }
            }
            // top and bottom are not in the taxonomy
            Map<String, Taxonomy> newHeads = new HashMap<>(heads);
            Taxonomy t = new Taxonomy(top, topScore, Double.NaN, new ArrayList<Taxonomy>() {
                {
                    add(new Taxonomy(bottom, bottomScore, linkScore, new ArrayList<Taxonomy>(), "none"));
                }
            }, "none");
            newHeads.put(top, t);
            return new Solution(newHeads, topics, size + 2);
        }
    }

    /**
     * *
     * Check if the solution has completed
     *
     * @return true if the solution is valid
     */
    public boolean isComplete() {
        return size() == topics.size() && heads.size() == 1;
    }

    /**
     * Convert this to a taxonomy (if it complete)
     *
     * @return The complete taxonomy
     * @throws IllegalStateException If the solution is not complete
     */
    public Taxonomy toTaxonomy() {
        if (isComplete()) {
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
        return new Taxonomy(taxo.root, taxo.score, taxo.linkScore, newChildren, "none");
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
        return "Solution{" + "heads=" + heads + ", topics=" + topics + ", size=" + size + '}';
    }
}
