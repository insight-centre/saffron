package org.insightcentre.nlp.saffron.taxonomy.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class ConnectivityInspector {

    private final DirectedGraph graph;

    public ConnectivityInspector(DirectedGraph graph) {
        this.graph = graph;
    }

    public Set<Set<Node>> connectedSets() {
        final Map<Node, Set<Node>> sets = new HashMap<>();
        for(Node n : graph.nodes) {
            Set<Node> s = new TreeSet<>();
            s.add(n);
            sets.put(n, s);
        }
        for(Edge e : graph.edges.getAllEdges()) {
            Set<Node> s1 = sets.get(e.getFrom());
            if(!s1.contains(e.getTo())) {
                s1.addAll(sets.get(e.getTo()));
                sets.put(e.getTo(), s1);
            }
        }
        return new HashSet<>(sets.values());
    }


}
