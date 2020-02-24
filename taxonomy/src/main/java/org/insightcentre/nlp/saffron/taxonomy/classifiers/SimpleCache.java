package org.insightcentre.nlp.saffron.taxonomy.classifiers;


import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Function;

/**
 * A LRU cache for any objects. Please note that this is NOT thread-safe!
 *
 * @author John McCrae
 */
public class SimpleCache<E, F> {

    private static final class CacheEntry<E> {

        public final E i;
        public int age;

        public CacheEntry(E i, int age) {
            this.i = i;
            this.age = age;
        }
    }

    /**
     * SAM to compute a cached entry
     *
     * @param <E> The key type
     * @param <F> The value type
     */
    public static interface Get<E, F> {

        public F get(E e);
    }

    private final HashMap<E, CacheEntry<F>> data = new HashMap<>();
    private int age = 0;
    private final int capacity;
    private int tooOld = 0;

    /**
     * Create a cache
     *
     * @param capacity The maximum capacity
     */
    public SimpleCache(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Get the object for the key or use get to compute it
     *
     * @param e The key
     * @param get The value computer
     * @return The result of get.get(e) possibly from the cache
     */
    public F get(E e, Function<E, F> get) {
        synchronized (data) {
            if (data.containsKey(e)) {
                final CacheEntry<F> ce;
                ce = data.get(e);
                ce.age = age++;
                return ce.i;
            }
        }
        CacheEntry<F> ce = new CacheEntry(get.apply(e), age++);
        synchronized (data) {
            data.put(e, ce);
            while (data.size() > capacity) {
                tooOld += Math.max(1, capacity / 10);
                Iterator<CacheEntry<F>> iter = data.values().iterator();
                while (iter.hasNext()) {
                    if (iter.next().age < tooOld) {
                        iter.remove();
                    }
                }
            }
        }
        return ce.i;
    }

    /**
     * Invalidate the whole cache
     */
    public void clear() {
        if (age > 0) {
            data.clear();
            tooOld = 0;
            age = 0;
        }
    }

    /**
     * The number of currently cached values
     *
     * @return
     */
    public int size() {
        return data.size();
    }
}

