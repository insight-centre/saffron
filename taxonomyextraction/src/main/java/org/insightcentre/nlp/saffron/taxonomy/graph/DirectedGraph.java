package org.insightcentre.nlp.saffron.taxonomy.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A directed graph
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class DirectedGraph {
    public final List<Node> nodes;
    public final AdjacencyList edges;
    
    @JsonCreator
    public DirectedGraph(@JsonProperty("nodes") List<Node> nodes, 
        @JsonProperty("edges") AdjacencyList prunedEdges) {
        this.nodes = nodes;
        this.edges = prunedEdges;
    }

    public DirectedGraph() {
        this.nodes = new ArrayList<>();
        this.edges = new AdjacencyList();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public AdjacencyList getEdges() {
        return edges;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.nodes);
        hash = 53 * hash + Objects.hashCode(this.edges);
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
        final DirectedGraph other = (DirectedGraph) obj;
        if (!Objects.equals(this.nodes, other.nodes)) {
            return false;
        }
        if (!Objects.equals(this.edges, other.edges)) {
            return false;
        }
        return true;
    }

}
