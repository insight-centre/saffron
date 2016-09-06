package org.insightcentre.nlp.saffron.taxonomy.graph;

public class Node implements Comparable<Node> {

  private final int name;
  private final String topicString;
  private boolean visited = false; // used for Kosaraju's algorithm and
                                   // Edmonds's algorithm
  private int lowlink = -1; // used for Tarjan's algorithm
  private int index = -1; // used for Tarjan's algorithm

  public String getTopicString() {
    return topicString;
  }
  
  public boolean isVisited() {
    return visited;
  }

  public void setVisited(boolean visited) {
    this.visited = visited;
  }

  public int getLowlink() {
    return lowlink;
  }

  public void setLowlink(int lowlink) {
    this.lowlink = lowlink;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getName() {
    return name;
  }

  public Node(final int argName, final String argPreferredString) {
    name = argName;
    topicString = argPreferredString;
  }

  public int compareTo(final Node argNode) {
    return argNode == this ? 0 : -1;
  }
}