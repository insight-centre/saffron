package org.insightcentre.nlp.saffron.taxonomy.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties({"visited"})
public class Node implements Comparable<Node> {

  private final int name;
  private final String topicString;
  private boolean visited = false; // used for Kosaraju's algorithm and
                                   // Edmonds's algorithm
//  private int lowlink = -1; // used for Tarjan's algorithm
//  private int index = -1; // used for Tarjan's algorithm

  public String getTopicString() {
    return topicString;
  }
  
  public boolean isVisited() {
    return visited;
  }

  public void setVisited(boolean visited) {
    this.visited = visited;
  }
//
//  public int getLowlink() {
//    return lowlink;
//  }
//
//  public void setLowlink(int lowlink) {
//    this.lowlink = lowlink;
//  }
//
//  public int getIndex() {
//    return index;
//  }
//
//  public void setIndex(int index) {
//    this.index = index;
//  }

  public int getName() {
    return name;
  }

  @JsonCreator
  public Node(@JsonProperty("name") final int argName, @JsonProperty("topicString") final String argPreferredString) {
    name = argName;
    topicString = argPreferredString;
  }

  public int compareTo(final Node argNode) {
    return argNode == this ? 0 : -1;
  }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.name;
        hash = 37 * hash + Objects.hashCode(this.topicString);
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
        final Node other = (Node) obj;
        if (this.name != other.name) {
            return false;
        }
        if (!Objects.equals(this.topicString, other.topicString)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Node{" + "name=" + name + ", topicString=" + topicString + '}';
    }

  
}