package org.insightcentre.nlp.saffron.taxonomy.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 * A directed graph
 * 
 * @param <Node> The type of the nodes
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class DirectedGraph<Node> {
    public final List<Node> nodes;
    public final List<Edge> edges;

    public DirectedGraph() {
        this.edges = Collections.EMPTY_LIST;
        this.nodes = Collections.EMPTY_LIST;
    }
    
    public DirectedGraph(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public Node getNode(int id) {
        return nodes.get(id);
    }

    public void addEdge(Node n1, Node n2, double weight) {
        int i = nodes.indexOf(n1);
        int j = nodes.indexOf(n2);
        edges.add(new Edge(i, j, weight));
    }

    public DirectedGraph<Node> filterBySet(List<Node> nodes) {
        int[] remap = new int[this.nodes.size()];
        for(int i = 0; i < this.nodes.size(); i++) {
            remap[i] = nodes.indexOf(this.nodes.get(i));
        }
        List<Edge> newEdges = new ArrayList<>();
        for(Edge e : edges) {
            if(remap[e.from] >=0 && remap[e.to] >= 0) {
                newEdges.add(new Edge(remap[e.from], remap[e.to], e.weight));
            }
        }

        return new DirectedGraph<>(nodes, newEdges);
        
    }

    public List<Edge> getOutgoing(Node n) {
        int i = nodes.indexOf(n);
        List<Edge> adjs = new ArrayList<>();
        for(Edge e : edges) {
            if(e.from == i) {
                adjs.add(e);
            }
        }
        return adjs;
    }

    public List<Edge> getIncoming(Node n) {
        int i = nodes.indexOf(n);
        List<Edge> adjs = new ArrayList<>();
        for(Edge e : edges) {
            if(e.to == i) {
                adjs.add(e);
            }
        }
        return adjs;
 
    }

    public Set<Node> getRoots() {
        Set<Node> nonRoots = new HashSet<>();
        Set<Node> all      = new HashSet<>();
        for(Edge e : edges) {
            Node from = getNode(e.from);
            Node to   = getNode(e.to);
            nonRoots.add(to);
            all.add(from);
        }
        all.removeAll(nonRoots);
        return all;
    }

    public static class Edge {
        private final int from, to;
        private final double weight;

        public Edge(int from, int to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }

        public double getWeight() {
            return weight;
        }



    }
}
