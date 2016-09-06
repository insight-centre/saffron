package org.insightcentre.nlp.saffron.taxonomy;

import org.insightcentre.nlp.saffron.taxonomy.graph.Edge;
import org.insightcentre.nlp.saffron.taxonomy.graph.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdjacencyList {

  private Map<Node, List<Edge>> adjacencies = new HashMap<Node, List<Edge>>();

  public void addEdge(Node source, Node target, double weight) {
    List<Edge> list;
    if (!adjacencies.containsKey(source)) {
      list = new ArrayList<Edge>();
      adjacencies.put(source, list);
    } else {
      list = adjacencies.get(source);
    }
    list.add(new Edge(source, target, weight));
  }

  public List<Edge> getAdjacent(Node source) {
    return adjacencies.get(source);
  }

  public void reverseEdge(Edge e) {
    adjacencies.get(e.getFrom()).remove(e);
    addEdge(e.getTo(), e.getFrom(), e.getWeight());
  }

  public void reverseGraph() {
    adjacencies = getReversedList().adjacencies;
  }

  public AdjacencyList getReversedList() {
    AdjacencyList newlist = new AdjacencyList();
    for (List<Edge> edges : adjacencies.values()) {
      for (Edge e : edges) {
        newlist.addEdge(e.getTo(), e.getFrom(), e.getWeight());
      }
    }
    return newlist;
  }

  public Set<Node> getSourceNodeSet() {
    return adjacencies.keySet();
  }

  public Collection<Edge> getAllEdges() {
    List<Edge> edges = new ArrayList<Edge>();
    for (List<Edge> e : adjacencies.values()) {
      edges.addAll(e);
    }
    return edges;
  }
}
