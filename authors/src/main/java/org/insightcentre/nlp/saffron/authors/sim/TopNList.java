package org.insightcentre.nlp.saffron.authors.sim;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A list containing at most the top N highest scoring elements
 * 
 * @param <A> the type of the item contained in the list
 * @author John McCrae
 */
public class TopNList<A extends Comparable<A>> extends AbstractCollection<A> {
    private static class Entry<A extends Comparable<A>> implements Comparable<Entry<A>>
    {
        final A value;
        final double score;

        public Entry(A value, double score) {
            this.value = value;
            this.score = score;
        }

        @Override
        public int compareTo(Entry<A> o) {
            int i = Double.compare(this.score, o.score);
            if(i != 0)
                return -i;
            return value.compareTo(o.value);
        }
        
        
    }
    
    private int size = 0;
    private final int capacity;
    private Entry<A>[] elems;

    /**
     * Create the list
     * @param capacity The maximum number of values to hold (N) 
     */
    public TopNList(int capacity) {
        if(capacity < 0)
            throw new IllegalArgumentException("Capacity cannot be negative");
        this.capacity = capacity;
    }

    @Override
    public int size() {
        return size;
    }
    
    /**
     * Add an element into this top N list
     * @param a The element
     * @param score Its score
     * @return true if the element was accepted into the list
     */
    public boolean offer(A a, double score) {
        if(elems == null) elems = new Entry[capacity];
        if(size < capacity) {
            elems[size++] = new Entry(a, score);
            if(size == capacity) {
                Arrays.sort(elems);
            }
            return true;
        } else {
            Entry e = new Entry(a, score);
            int idx = Arrays.binarySearch(elems, e);
            if(idx < 0) {
                idx = -idx - 1;
            }
            if(idx < capacity) {
                if(idx < capacity - 1) {
                    System.arraycopy(elems, idx, elems, idx + 1, capacity - idx - 1);
                }
                elems[idx] = e;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public Iterator<A> iterator() {
        return new Iterator<A>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public A next() {
                if(i >= size)
                    throw new NoSuchElementException();
                return elems[i++].value;
            }
        };
    }
    
    
    

}
