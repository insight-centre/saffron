package org.insightcentre.nlp.saffron.taxonomy.graph;

import org.insightcentre.nlp.saffron.taxonomy.AdjacencyList;
import org.insightcentre.nlp.saffron.taxonomy.graphviz.SaffronDotExporter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

public class JGraphTRepresentation {

  public static DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> convertToJGraphT(
      List<Node> nodes, AdjacencyList edgeList) {
    DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> directedGraph =
        new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(
            DefaultWeightedEdge.class);

    for (Node node : nodes) {

      String nodeString = node.getTopicString();
      // nodeString = Topic.get(nodeString).getPreferredString();
      directedGraph.addVertex(nodeString);
    }

    Collection<Edge> edges = edgeList.getAllEdges();
    for (Edge edge : edges) {
      if (edge != null) {
        // System.out.println(edge.getFrom().getTopicString() + ", "
        // + edge.getTo().getTopicString());

        String edgeToString = edge.getTo().getTopicString();
        String edgeFromString = edge.getFrom().getTopicString();

        // edgeToString = Topic.get(edgeToString).getPreferredString();
        // edgeFromString = Topic.get(edgeFromString).getPreferredString();

        if ((edgeFromString != edgeToString) && (edge.getWeight() > 0)) {

          DefaultWeightedEdge e =
              directedGraph.addEdge(edgeFromString, edgeToString);

          if (e != null) {
            directedGraph.setEdgeWeight(e, edge.getWeight());
          }
        }
      }
    }

    return directedGraph;
  }

  public static void exportToDot(
      DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph,
      String fileName, Map<String, Double> sumPMIMap) throws SQLException, IOException {
    SaffronDotExporter<String, DefaultWeightedEdge> de =
        new SaffronDotExporter<String, DefaultWeightedEdge>(
            new StringNameProvider<String>(), null, null);

      FileWriter w = new FileWriter(fileName);
      de.export(w, graph, sumPMIMap);
      w.close();
  }
  
  public static String taxonomyTreeText(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph,
	      Map<String, Double> sumPMIMap) {
	  
	  //Find the roots
	  //TODO: There can be more than one root!
	  List<String> roots = new ArrayList<>();
	  for (String node : graph.vertexSet()) {
		  if (graph.inDegreeOf(node)==0) {
			  roots.add(node);
		  }
	  }
	  
	  StringBuilder tree = new StringBuilder();
	  for (String root : roots) {
		  taxonomyTreeTraverser(root, 0, tree, graph);
	  }
	  return tree.toString();
  }
  
  private static void taxonomyTreeTraverser(
	String node, int depth, StringBuilder tree,
  	DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph
  ) {
	  for (int i=0; i<depth; i++) {
		  tree.append("    ");
	  }
	  tree.append(node + "\n");
	  
	  for (DefaultWeightedEdge edge : graph.outgoingEdgesOf(node)) {
		  String child = graph.getEdgeTarget(edge);
		  taxonomyTreeTraverser(child, depth+1, tree, graph);
	  }
	  
  }
}
