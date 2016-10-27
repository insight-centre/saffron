package org.insightcentre.nlp.saffron.taxonomy.graph;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphPruning {

  static AdjacencyList aggregatedList = new AdjacencyList();


  /**
   * @param nodes
   * @param edges
   */
  public static AdjacencyList pruneGraph(List<Node> nodes, AdjacencyList edges,
      Map<String, Double> pmiMap) {

      DirectedGraph graph = new DirectedGraph(nodes, edges);

      ConnectivityInspector inspector = new ConnectivityInspector(graph);

    int i = 0;
    for (Set<Node> strong : inspector.connectedSets()) {

      i++;
     // logger.log(Level.INFO, "Pruning component " + i + "..");
      List<Node> componentNodes = filterNodes(nodes, strong);
      AdjacencyList filteredEdges = filterEdges(edges, strong);

      if (componentNodes.size() > 1) {

        Node root = findRootName(pmiMap, componentNodes);

        // Node root = findRankedRootName(pmiMap, componentNodes,
        // filteredEdges);
        // Node root = nodes.get(findRootName(filteredEdges, componentNodes));
        //logger
        //    .log(Level.INFO, "Found component root: " + root.getTopicString());

        // remove incoming edges for root, this step might create other
        // subcomponents
        AdjacencyList cleanedList = new AdjacencyList();
        Collection<Edge> edgeCollection = filteredEdges.getAllEdges();
        for (Edge edge : edgeCollection) {
          if (edge.getTo() != root) {
            cleanedList.addEdge(edge.getFrom(), edge.getTo(), edge.getWeight());
          }
        }

        cleanedList = disconnectFalseRoots(cleanedList, componentNodes, root);
        // connectFalseRootsToRoot(cleanedList, componentNodes, root);

        DirectedGraph tmpGraph = new DirectedGraph(componentNodes, cleanedList);
        ConnectivityInspector tmpInspector = new ConnectivityInspector(tmpGraph);

        if (tmpInspector.connectedSets().size() == 1) {

          //logger.log(Level.INFO, "Finding the minimum spanning tree..");
          Edmonds ed = new Edmonds();
          AdjacencyList resultList = ed.getMaxBranching(root, cleanedList);

          aggregatedList = aggregateLists(aggregatedList, resultList);
        } else {

          for (Set<Node> component : tmpInspector.connectedSets()) {
            i++;
            //logger.log(Level.INFO, "Pruning component " + i + "..");
            List<Node> cNodes = filterNodes(componentNodes, component);
            AdjacencyList fEdges = filterEdges(cleanedList, component);

            if (cNodes.size() > 1) {
              pruneGraph(cNodes, fEdges, pmiMap);
            }
          }
        }
      } else {
        //logger.log(Level.INFO, "The component has size one ..");
      }
    }

    return aggregatedList;
  }

  private static List<Node> filterNodes(List<Node> allNodes,
      Set<Node> nodeStrings) {

    List<Node> filteredNodes = new ArrayList<Node>();

    for (Node node : allNodes) {
      if (nodeStrings.contains(node)) {
        filteredNodes.add(node);
      }
    }
    return filteredNodes;
  }

  private static AdjacencyList filterEdges(AdjacencyList edges,
      Set<Node> nodeStrings) {

    AdjacencyList filteredEdges = new AdjacencyList();
    Collection<Edge> allEdges = edges.getAllEdges();

    for (Edge edge : allEdges) {
      if (nodeStrings.contains(edge.getFrom())
          && nodeStrings.contains(edge.getTo())) {
        filteredEdges.addEdge(edge.getFrom(), edge.getTo(), edge.getWeight());
      }
    }
    return filteredEdges;
  }

  private static Integer findRootName(AdjacencyList list, List<Node> nodes) {
    Collection<Edge> edges = list.getAllEdges();
    List<Integer> outgoingLinks = new ArrayList<Integer>();

    for (Node node : nodes) {
      outgoingLinks.add(node.getName(), 0);
    }

    for (Edge edge : edges) {
      int source = edge.getFrom().getName();
      int value = outgoingLinks.get(source) + 1;
      outgoingLinks.remove(source);
      outgoingLinks.add(source, value);
    }
    Integer max = 0;
    Integer root = 0;
    for (Integer l : outgoingLinks) {
      if (l > max) {
        max = l;
        root = outgoingLinks.indexOf(l);
      }
    }

    return root;
  }

  private static AdjacencyList disconnectFalseRoots(AdjacencyList list,
      List<Node> nodes, Node root) {
    Collection<Edge> edges = list.getAllEdges();
    Map<Integer, Integer> incomingLinks = new HashMap<Integer, Integer>();

    AdjacencyList cleanedList = new AdjacencyList();

    // intialise the number of incoming links for each node
    for (Node node : nodes) {
      incomingLinks.put(node.getName(), 0);
    }

    // compute the number of incoming links for each node
    for (Edge edge : edges) {
      int dest = edge.getTo().getName();
      int val = incomingLinks.get(dest) + 1;
      incomingLinks.remove(dest);
      incomingLinks.put(dest, val);
    }

    for (Edge edge : edges) {
      Node nodeFrom = edge.getFrom();
      Node nodeTo = edge.getTo();

      if ((nodeFrom.getName() != root.getName())
          && (incomingLinks.get(nodeFrom.getName()) == null)
          && (incomingLinks.get(nodeTo.getName()) == null)
          && (incomingLinks.get(nodeFrom.getName()) == 0)) {
        //logger.log(Level.INFO, "Cleaning edge: " + nodeFrom.getTopicString()
        //    + " -> " + edge.getTo().getTopicString() + ", for false root "
        //    + nodeFrom.getTopicString());
      } else {
        cleanedList.addEdge(nodeFrom, edge.getTo(), edge.getWeight());
      }
    }

    return cleanedList;
  }

  private static Node findRootName(Map<String, Double> pmiMap, List<Node> nodes) {

    Double maxSumPMI = 0.0;
    Node root = null;

    for (Node nodei : nodes) {

      String key = nodei.getTopicString();

      if (pmiMap.get(key) > maxSumPMI) {
        maxSumPMI = pmiMap.get(key);
        root = nodei;
      }
    }

    if (root == null) {
      root = nodes.get(0);
    }

    return root;
  }

  private static AdjacencyList aggregateLists(AdjacencyList l1, AdjacencyList l2) {
    Collection<Edge> l2Edges = l2.getAllEdges();

    for (Edge edge : l2Edges) {
      l1.addEdge(edge.getFrom(), edge.getTo(), edge.getWeight());
    }

    return l1;
  }

  private static AdjacencyList pruneGraphTest(List<Node> nodes,
      AdjacencyList edges, Map<String, Double> pmiMap) {
    Collection<Edge> edgeCollection = edges.getAllEdges();

    AdjacencyList cleanedList = new AdjacencyList();
    Node root = findRootName(pmiMap, nodes);
    // Node root = nodes.get(findRootName(edges, nodes));
    //logger.log(Level.INFO, "The root is: " + root.getTopicString());

    // remove incoming edges for root
    for (Edge edge : edgeCollection) {
      if (edge.getTo() != root) {
        cleanedList.addEdge(edge.getFrom(), edge.getTo(), edge.getWeight());
      }
    }

    // remove false roots
    cleanedList = disconnectFalseRoots(cleanedList, nodes, root);

    //logger.log(Level.INFO, "Finding the minimum spanning tree..");
    Edmonds ed = new Edmonds();
    AdjacencyList resultList = ed.getMaxBranching(root, cleanedList);
    return resultList;
  }

  public static AdjacencyList pruneMultipleParents(List<Node> nodes,
      AdjacencyList edges) {

    GraphPruning gp = new GraphPruning();
    AdjacencyList cleanedEdges = new AdjacencyList();

    for (Node node : nodes) {
      List<Edge> adjacentEdges = edges.getAdjacent(node);

      if (adjacentEdges != null) {
        List<Edge> incoming = gp.incomingEdges(node, adjacentEdges);

        if (incoming != null) {
          Edge incomingEdge = gp.findMaxWeightEdge(incoming);
          if ((incomingEdge != null)
              && (incomingEdge.getFrom() != incomingEdge.getTo())) {
            cleanedEdges.addEdge(incomingEdge.getFrom(), incomingEdge.getTo(),
                incomingEdge.getWeight());
          }
        }

        List<Edge> outgoing = gp.outgoingEdges(node, adjacentEdges);
        if (outgoing != null) {
          for (Edge outgoingEdge : outgoing) {
            if (outgoingEdge.getFrom() != outgoingEdge.getTo()) {
              cleanedEdges.addEdge(outgoingEdge.getFrom(),
                  outgoingEdge.getTo(), outgoingEdge.getWeight());
            }
          }
        }
      }
    }

    // TODO remove parallel edges

    return cleanedEdges;
  }

  private List<Edge> incomingEdges(Node node, List<Edge> adjacentEdges) {
    List<Edge> incoming = new ArrayList<Edge>();

    for (Edge edge : adjacentEdges) {
      Node source = edge.getFrom();

      if (node.getTopicString() != source.getTopicString()) {
        incoming.add(edge);
      }
    }

    return incoming;
  }

  private List<Edge> outgoingEdges(Node node, List<Edge> adjacentEdges) {
    List<Edge> outgoing = new ArrayList<Edge>();

    for (Edge edge : adjacentEdges) {
      Node dest = edge.getTo();

      if (node.getTopicString() != dest.getTopicString()) {
        outgoing.add(edge);
      }
    }

    return outgoing;
  }

  private Edge findMaxWeightEdge(List<Edge> incomingEdges) {

    if (incomingEdges.size() > 0) {
      Edge maxWeightEdge = incomingEdges.get(0);

      for (Edge edge : incomingEdges) {
        if (edge.getWeight() > maxWeightEdge.getWeight()) {
          maxWeightEdge = edge;
        } else if (edge.getWeight() == maxWeightEdge.getWeight()) {
          // in case the edges have the same weight keep the longer one
          if (edge.getFrom().getTopicString().length() > maxWeightEdge
              .getFrom().getTopicString().length()) {
            maxWeightEdge = edge;
          }
        }
      }

      return maxWeightEdge;
    }

    return null;
  }
}
