package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import org.insightcentre.nlp.saffron.data.TypedLink;

/**
 *
 * @author John McCrae
 */
public class GreedyKGList extends AbstractCollection<TypedLink> {

    private final List<TypedLink> links;
    private final Integer[] orderedIndices;
    private final double[] scores;

    public GreedyKGList(List<TypedLink> links) {
        this.links = links;
        this.orderedIndices = new Integer[links.size()];
        for (int i = 0; i < links.size(); i++) {
            orderedIndices[i] = i;
        }
        this.scores = new double[links.size()];
    }

    @Override
    public boolean remove(Object o) {
        for (int i = 0; i < orderedIndices.length; i++) {
            if (orderedIndices[i] >= 0 && links.get(orderedIndices[i]).equals(o)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Apply a score to all elements of the list and sort this list according to
     * this element
     *
     * @param scorer
     */
    public void scoreAndSort(ToDoubleFunction<TypedLink> scorer) {
        for (Integer idx : orderedIndices) {
            if (idx >= 0) {
                scores[idx] = scorer.applyAsDouble(links.get(idx));
            }
        }

        Arrays.sort(orderedIndices, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                if (i1 < 0 && i2 < 0) {
                    return Integer.compare(i1, i2);
                } else if (i1 < 0) {
                    return +1;
                } else if (i2 < 0) {
                    return -1;
                }

                TypedLink o1 = links.get(i1);
                TypedLink o2 = links.get(i2);
                //Consider synonyms as the most important links

                if (o1.getType().equals(TypedLink.Type.synonymy)
                        && !o2.getType().equals(TypedLink.Type.synonymy)) {
                    return Integer.MIN_VALUE;
                } else if (!o1.getType().equals(TypedLink.Type.synonymy)
                        && o2.getType().equals(TypedLink.Type.synonymy)) {
                    return Integer.MAX_VALUE;
                }
                int c = Double.compare(scores[i1], scores[i2]);
                return c == 0 ? i1.compareTo(i2) : -c;
            }
        });
    }

    public TypedLink remove(int i) {
        TypedLink tl = links.get(orderedIndices[i]);
        Integer i2 = -orderedIndices[i] - 1;
        System.arraycopy(orderedIndices, i + 1, orderedIndices, i, orderedIndices.length - i - 1);
        orderedIndices[orderedIndices.length - 1] = i2;
        return tl;
    }

    @Override
    public Iterator<TypedLink> iterator() {
        return new Iterator<TypedLink>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                while (i < orderedIndices.length && orderedIndices[i] < 0) {
                    i++;
                }
                return i < orderedIndices.length;
            }

            @Override
            public TypedLink next() {
                while (i < orderedIndices.length && orderedIndices[i] < 0) {
                    i++;
                }
                return links.get(orderedIndices[i++]);
            }
        };
    }

    @Override
    public boolean isEmpty() {
        for (Integer idx : orderedIndices) {
            if (idx >= 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        throw new RuntimeException("Slow don't do this");
    }

    public double getScore(int i) {
        return scores[orderedIndices[i] < 0 ? -1 - orderedIndices[i] : orderedIndices[i]];
    }
}
