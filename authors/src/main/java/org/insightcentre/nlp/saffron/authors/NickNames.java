package org.insightcentre.nlp.saffron.authors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class NickNames {

    public final static Map<String, Set<String>> first_nicks = new HashMap<>();

    static {
        loadMap();
    }

    private static void loadMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Collection> o;
        try {
            o = mapper.readValue(NickNames.class.getResource("/names.json"), Map.class);
        } catch (IOException ex) {
            System.err.println("Could not load nick names");
            return;
        }
        for(Map.Entry<String, Collection> e : o.entrySet()) {
            first_nicks.put(e.getKey(), new TreeSet<String>(e.getValue()));
        }
    }
}