package org.insightcentre.nlp.saffron.topic.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
class EmbeddednessCalculator {

    private Iterable<StringTuple> variable_sliding_window(final StringTuple topic_seq, final int max) {
        return new Iterable<StringTuple>() {

            @Override
            public Iterator<StringTuple> iterator() {
                return new Iterator<StringTuple>() {
                    int i = 0;
                    int j = 1;

                    @Override
                    public boolean hasNext() {
                        return i < max;
                    }

                    @Override
                    public StringTuple next() {
                        StringTuple st = new StringTuple(Arrays.copyOfRange(topic_seq.s, i , j));
                        j++;
                        if(j > max) {
                            i++;
                            j = i + 1;
                        }
                        return st;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported");
                    }
                };
            }
            

        };
    }
    private static class StringTuple {
        private final String[] s;

        public StringTuple(String[] s) {
            this.s = s;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Arrays.deepHashCode(this.s);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StringTuple other = (StringTuple) obj;
            if (!Arrays.deepEquals(this.s, other.s)) {
                return false;
            }
            return true;
        }

        public int len() { return s.length; }

    }
    
    private final Map<StringTuple, Topic> topic_map = new HashMap<>();
    void add(String[] tokens, Topic topic) {
        topic_map.put(new StringTuple(tokens), topic);
    }
        /**
        Iterate over every possible sequence of tokens in each topic, and check
        if that embedded topic exists in the topic map.
        Not as slow as it sounds as topics are usually no more than 5 tokens long.
        */
    Map<String, List<Topic>> calculate() {
       Map<String, List<Topic>> embeddedness_map = new HashMap<>();
        for(StringTuple topic_seq : topic_map.keySet()) {
            for(StringTuple key : variable_sliding_window(topic_seq, topic_seq.len())) {
                if(topic_map.containsKey(key)) {
                    Topic embedded_topic = topic_map.get(key);
                    if(!embeddedness_map.containsKey(embedded_topic.topicString)) 
                        embeddedness_map.put(embedded_topic.topicString, new ArrayList<Topic>());
                    embeddedness_map.get(embedded_topic.topicString).add(topic_map.get(topic_seq));
                }
            }
        }
        return embeddedness_map;
    }

}
