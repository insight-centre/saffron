package org.insightcentre.nlp.saffron.taxonomy.graph;


public class TmpEdge {

    private Node node1;
    private Node node2;
    private Double weight;

    public TmpEdge(Node node1, Node node2, Double weight) {
      super();
      this.node1 = node1;
      this.node2 = node2;
      this.weight = weight;
    }

    public Node getNode1() {
      return node1;
    }

    public void setNode1(Node node1) {
      this.node1 = node1;
    }

    public Node getNode2() {
      return node2;
    }

    public void setNode2(Node node2) {
      this.node2 = node2;
    }

    public Double getWeight() {
      return weight;
    }

    public void setWeight(Double weight) {
      this.weight = weight;
    }

}
