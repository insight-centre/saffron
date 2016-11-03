package org.insightcentre.nlp.saffron.taxonomy.graph;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.insightcentre.nlp.saffron.taxonomy.graph.DirectedGraph.Edge;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class GraphPruning {

    public static <Node> DirectedGraph<Node> pruneGraph(DirectedGraph<Node> graph, Map<Node, Double> pmiMap) {

        List<Edge> aggregatedList = new ArrayList<>();

        int i = 0;
        for (Set<Node> strong : connectedSets(graph)) {

            i++;
            // logger.log(Level.INFO, "Pruning component " + i + "..");
            List<Node> componentNodes = filterNodes(graph.getNodes(), strong);
            List<Edge> filteredEdges = filterEdges(graph.getEdges(), graph, strong);

            if (componentNodes.size() > 1) {

                Node root = findRootName(pmiMap, componentNodes);

                // Node root = findRankedRootName(pmiMap, componentNodes,
                // filteredEdges);
                // Node root = nodes.get(findRootName(filteredEdges, componentNodes));
                //logger
                //    .log(Level.INFO, "Found component root: " + root.getTopicString());
                // remove incoming edges for root, this step might create other
                // subcomponents
                List<Edge> cleanedList = new ArrayList<>();
                for (Edge edge : filteredEdges) {
                    Node to = graph.getNode(edge.getTo());
                    if (to.equals(root)) {
                        cleanedList.add(new Edge(edge.getFrom(), edge.getTo(), edge.getWeight()));
                    }
                }

                cleanedList = disconnectFalseRoots(new DirectedGraph(graph.nodes, cleanedList), root);
                // connectFalseRootsToRoot(cleanedList, componentNodes, root);

                DirectedGraph tmpGraph = new DirectedGraph(graph.nodes, cleanedList).filterBySet(componentNodes);
                Set<Set<Node>> sets = connectedSets(tmpGraph);

                if (sets.size() == 1) {

                    //logger.log(Level.INFO, "Finding the minimum spanning tree..");
                    Edmonds ed = new Edmonds();
                    List<Edge> resultList = ed.getMaxBranching(root, tmpGraph, cleanedList);

                    aggregatedList.addAll(resultList);
                } else {

                    for (Set<Node> component : sets) {
                        i++;
                        //logger.log(Level.INFO, "Pruning component " + i + "..");
                        List<Node> cNodes = filterNodes(componentNodes, component);
                        List<Edge> fEdges = filterEdges(cleanedList, tmpGraph, component);

                        if (cNodes.size() > 1) {
                            pruneGraph(new DirectedGraph(tmpGraph.edges, fEdges).filterBySet(cNodes), pmiMap);
                        }
                    }
                }
            } else {
                //logger.log(Level.INFO, "The component has size one ..");
            }
        }

        return new DirectedGraph<>(graph.getNodes(), aggregatedList);
    }

    public static <Node> DirectedGraph<Node> pruneMultipleParents(List<Node> nodes, DirectedGraph<Node> graph) {

        List<Edge> cleanedEdges = new ArrayList<>();

        for (Node node : nodes) {
            List<Edge> incoming = graph.getIncoming(node);

            if (incoming != null) {
                Edge incomingEdge = findMaxWeightEdge(incoming);
                if ((incomingEdge != null)
                    && (incomingEdge.getFrom() != incomingEdge.getTo())) {
                    cleanedEdges.add(incomingEdge);
                }
            }

            List<Edge> outgoing = graph.getOutgoing(node);
            if (outgoing != null) {
                for (Edge outgoingEdge : outgoing) {
                    if (outgoingEdge.getFrom() != outgoingEdge.getTo()) {
                        cleanedEdges.add(outgoingEdge);
                    }
                }
            }
        }

        // TODO remove parallel edges
        return new DirectedGraph<>(nodes, cleanedEdges);
    }

    public static <Node> Set<Set<Node>> connectedSets(DirectedGraph<Node> graph) {
        final Map<Node, Set<Node>> sets = new HashMap<>();
        for (Node n : graph.nodes) {
            Set<Node> s = new TreeSet<>();
            s.add(n);
            sets.put(n, s);
        }
        for (Edge e : graph.edges) {
            Node n1 = graph.getNode(e.getFrom());
            Node n2 = graph.getNode(e.getTo());
            Set<Node> s1 = sets.get(n1);
            if (!s1.contains(n2)) {
                s1.addAll(sets.get(n2));
                sets.put(n2, s1);
            }
        }
        return new HashSet<>(sets.values());
    }

    private static <Node> List<Node> filterNodes(List<Node> allNodes,
        Set<Node> nodeStrings) {

        List<Node> filteredNodes = new ArrayList<Node>();

        for (Node node : allNodes) {
            if (nodeStrings.contains(node)) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    private static <Node> List<Edge> filterEdges(List<Edge> edges,
        DirectedGraph<Node> graph, Set<Node> nodes) {

        List<Edge> filteredEdges = new ArrayList<>();

        for (Edge edge : edges) {
            Node from = graph.getNode(edge.getFrom());
            Node to = graph.getNode(edge.getTo());
            if (nodes.contains(from) && nodes.contains(to)) {
                filteredEdges.add(new Edge(edge.getFrom(), edge.getTo(), edge.getWeight()));
            }
        }
        return filteredEdges;
    }

    private static <Node> Node findRootName(Map<Node, Double> pmiMap, List<Node> nodes) {

        double maxSumPMI = Double.NEGATIVE_INFINITY;
        Node root = null;

        for (Node nodei : nodes) {

            if (pmiMap.get(nodei) > maxSumPMI) {
                maxSumPMI = pmiMap.get(nodei);
                root = nodei;
            }
        }

        if (root == null) {
            root = nodes.get(0);
        }

        return root;
    }

    private static <Node> List<Edge> disconnectFalseRoots(DirectedGraph<Node> graph, Node root) {
        Collection<Edge> edges = graph.edges;
        Object2IntMap<Node> incomingLinks = new Object2IntOpenHashMap<>();

        List<Edge> cleanedList = new ArrayList<>();

        // compute the number of incoming links for each node
        for (Edge edge : edges) {
            Node dest = graph.getNode(edge.getTo());
            incomingLinks.put(dest, incomingLinks.getInt(dest) + 1);
        }

        for (Edge edge : edges) {
            Node nodeFrom = graph.getNode(edge.getFrom());

            if (nodeFrom.equals(root) || incomingLinks.get(nodeFrom) > 0) {
                cleanedList.add(edge);
            }
        }

        return cleanedList;
    }

    private static Edge findMaxWeightEdge(List<Edge> incomingEdges) {

        if (incomingEdges.size() > 0) {
            Edge maxWeightEdge = incomingEdges.get(0);

            for (Edge edge : incomingEdges) {
                if (edge.getWeight() > maxWeightEdge.getWeight()) {
                    maxWeightEdge = edge;
                } else if (edge.getWeight() == maxWeightEdge.getWeight()) {
//          // in case the edges have the same weight keep the longer one
//          if (edge.getFrom().getTopicString().length() > maxWeightEdge
//              .getFrom().getTopicString().length()) {
                    maxWeightEdge = edge;
                }
            }

            return maxWeightEdge;
        }
        return null;
    }
}
