package org.insightcentre.nlp.saffron.taxonomy.supervised;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * A pretty good tokenizer
 *
 * @author John McCrae
 */
public final class PrettyGoodTokenizer {

    private final static Pattern pattern1 = Pattern.compile("(\\.\\.\\.+|[\\p{Po}\\p{Ps}\\p{Pe}\\p{Pi}\\p{Pf}\u2013\u2014\u2015&&[^'\\.]]|(?<!(\\.|\\.\\p{L}))\\.(?=[\\p{Z}\\p{Pf}\\p{Pe}]|\\Z)|(?<!\\p{L})'(?!\\p{L}))");
    private final static Pattern pattern2 = Pattern.compile("\\p{C}|^\\p{Z}+|\\p{Z}+$");

    private final static SimpleCache<String, String[]> cache = new SimpleCache(10000);

    private final static class DoTokenize implements SimpleCache.Get<String, String[]> {

        @Override
        public String[] get(String s) {
            String s1 = pattern1.matcher(s).replaceAll(" $1 ");
            String s2 = pattern2.matcher(s1).replaceAll("");
            return s2.split("\\p{Z}+");
        }

    }
    private final static DoTokenize doTokenize = new DoTokenize();

    public static String[] tokenize(String s) {
        return cache.get(s, doTokenize);
    }

    /**
     * A LRU cache for any objects
     *
     * @author John McCrae
     */
    public static class SimpleCache<E, F> {

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
        private final int threshold;
        private int tooOld = 0;

        /**
         * Create a cache
         *
         * @param capacity The maximum capacity
         */
        public SimpleCache(int capacity) {
            this.capacity = capacity;
            this.threshold = capacity * 10 / 9;
        }

        /**
         * Get the object for the key or use get to compute it
         *
         * @param e The key
         * @param get The value computer
         * @return The result of get.get(e) possibly from the cache
         */
        public F get(E e, Get<E, F> get) {
            if (data.containsKey(e)) {
                CacheEntry<F> ce = data.get(e);
                ce.age = age++;
                return ce.i;
            } else {
                CacheEntry<F> ce = new CacheEntry(get.get(e), age++);
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
                return ce.i;
            }
        }
    }

}
