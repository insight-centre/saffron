package org.insightcentre.nlp.saffron.term;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Count whether a term is more likely to be upper cased
 * @author John McCrae
 */
public class CasingStats {
    public final HashMap<String, Object2IntOpenHashMap<String>> casing = new HashMap<>();
    
    public void addCasing(String term) {
        String termLc = term.toLowerCase();
        if(!casing.containsKey(term)) {
            casing.put(termLc, new Object2IntOpenHashMap<String>());
        }
        casing.get(termLc).put(term, casing.get(termLc).getInt(term) + 1);
    }

    public String trueCase(String term) {
        String[] tokens = term.split(" ");
        StringBuilder sb = new StringBuilder();
        for(String tk : tokens) {
            if(sb.length() != 0) {
                sb.append(" ");
            }
            if(casing.containsKey(tk)) {
                String best = tk;
                int bestCount = -1;
                for(Object2IntMap.Entry<String> e : casing.get(tk).object2IntEntrySet()) {
                    if(e.getIntValue() > bestCount) {
                        best = e.getKey();
                        bestCount = e.getIntValue();
                    }
                }
                sb.append(best);
            } else {
                sb.append(tk);
            }
        }
        return sb.toString();
    }
    
    public void add(CasingStats other) {
        for(Map.Entry<String, Object2IntOpenHashMap<String>> e : other.casing.entrySet()) {
            if(!casing.containsKey(e.getKey())) {
                casing.put(e.getKey(), new Object2IntOpenHashMap<String>());
            }
            for(Object2IntMap.Entry<String> e2 : e.getValue().object2IntEntrySet()) {
                casing.get(e.getKey()).put(e2.getKey(), e2.getIntValue() + casing.get(e.getKey()).getInt(e2.getKey()));
            }
        }
    }

    @Override
    public String toString() {
        return "CasingStats{" + "casing=" + casing + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.casing);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CasingStats other = (CasingStats) obj;
        if (!Objects.equals(this.casing, other.casing)) {
            return false;
        }
        return true;
    }
    
    
}
