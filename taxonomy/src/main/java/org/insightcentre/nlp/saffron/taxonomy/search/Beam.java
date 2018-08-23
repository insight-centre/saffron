package org.insightcentre.nlp.saffron.taxonomy.search;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ObjectHeaps;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * A beam is a heap which will reject values that are over a certain score
 * threshold. This beam is sorted in descending order
 *
 * @author John McCrae
 * @param <K> The type of object stored in the beam
 */
public class Beam<K extends Comparable<K>> {

    private final int maxSize;

    @SuppressWarnings("unchecked")
    protected transient K[] heap = (K[]) new Comparable[0];
    /**
     * The number of elements in this queue.
     */
    protected int size;
    /**
     * The type-specific comparator used in this queue.
     */
    protected final Comparator<? super K> c;
    private final Object2DoubleMap<K> scores;

    /**
     * Create a new beam
     *
     * @param maxSize The maximum number of elements to store in the beam
     */
    public Beam(int maxSize) {
        this.maxSize = maxSize;
        this.scores = new Object2DoubleOpenHashMap<>();
        this.c = new BeamComparator();
    }

    /**
     * Add a single object to the beam
     *
     * @param k The object
     * @param score The score of the object
     * @return True if the object was actually stored in the beam
     */
    public boolean push(K k, double score) {
        if (size < maxSize) {
            scores.put(k, score);
            enqueue(k);
            return true;
        } else if (score > scores.getDouble(last())) {
            K k2 = heap[size - 1];
            heap[size - 1] = k;
            scores.put(k, score);
            ObjectHeaps.upHeap(heap, size, size - 1, c);
            scores.remove(k2);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Is this queue empty
     *
     * @return True if the queue is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Remove the highest scoring element from this beam
     *
     * @return The highest scoring element in this beam
     * @throws NoSuchElementException If the beam is empty
     */
    public K pop() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        final K result = heap[0];
        heap[0] = heap[--size];
        heap[size] = null;
        if (size != 0) {
            ObjectHeaps.downHeap(heap, size, 0, c);
        }
        return result;
    }

    private class BeamComparator implements Comparator<K> {

        @Override
        public int compare(K o1, K o2) {
            int c = Double.compare(scores.getDouble(o1), scores.getDouble(o2));
            return c == 0 ? o1.compareTo(o2) : -c;
        }

    }

    private K last() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return heap[size - 1];
    }

    private void enqueue(K x) {
        if (size == heap.length) {
            heap = ObjectArrays.grow(heap, size + 1);
        }
        heap[size++] = x;
        ObjectHeaps.upHeap(heap, size, size - 1, c);
    }
}
