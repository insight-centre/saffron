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
        return pruneGraph(graph, pmiMap, 0);
    }

    public static <Node> DirectedGraph<Node> pruneGraph(DirectedGraph<Node> graph, Map<Node, Double> pmiMap, int depth) {

        DirectedGraph<Node> aggregatedList = new DirectedGraph<>(graph.getNodes());

        int i = 0;
        Set<Set<Node>> connSets = connectedSets(graph);
        for(int j = 0; j < depth; j++) System.err.print("  ");
        System.err.printf("%d connected sets from %d nodes\n", connSets.size(), graph.nodes.size());
        for (Set<Node> strong : connSets) {

            i++;
            List<Node> componentNodes = new ArrayList<>(strong);
            // logger.log(Level.INFO, "Pruning component " + i + "..");
            DirectedGraph<Node> filteredEdges = filterEdges(graph, strong);
            //System.err.printf("%d edges after filtering\n", filteredEdges.getEdges().size());

            if (strong.size() > 1) {

                Node root = findRootName(pmiMap, strong);

                // Node root = findRankedRootName(pmiMap, componentNodes,
                // filteredEdges);
                // Node root = nodes.get(findRootName(filteredEdges, componentNodes));
                //logger
                //    .log(Level.INFO, "Found component root: " + root.getTopicString());
                // remove incoming edges for root, this step might create other
                // subcomponents
                DirectedGraph<Node> cleanedList = new DirectedGraph<>(componentNodes);
                for (Edge edge : filteredEdges.getEdges()) {
                    Node to = filteredEdges.getNode(edge.getTo());
                    Node from = filteredEdges.getNode(edge.getFrom());
                    if (!to.equals(root)) {
                        cleanedList.addEdge(to, from, edge.getWeight());
                       // (new Edge(edge.getFrom(), edge.getTo(), edge.getWeight()));
                    }
                }
                //System.err.printf("Cleaned to %d\n", cleanedList.getEdges().size());

                cleanedList = disconnectFalseRoots(cleanedList, root);
                // connectFalseRootsToRoot(cleanedList, componentNodes, root);
                //System.err.printf("Disconnected to %d\n", cleanedList.getEdges().size());

                DirectedGraph tmpGraph = cleanedList.filterBySet(componentNodes);
                Set<Set<Node>> sets = connectedSets(tmpGraph);
                //System.err.printf("Left %d connected sets\n", sets.size());

                if (sets.size() == 1) {

                    //logger.log(Level.INFO, "Finding the minimum spanning tree..");
                    Edmonds ed = new Edmonds();
                    DirectedGraph<Node> resultList = ed.getMaxBranching(root, tmpGraph, cleanedList);

                    aggregatedList.addAll(resultList);
                } else {

                    for (Set<Node> component : sets) {
                        i++;
                        //logger.log(Level.INFO, "Pruning component " + i + "..");
                        List<Node> cNodes = new ArrayList<>(component);
                        DirectedGraph<Node> fEdges = filterEdges(tmpGraph, component);

                        if (cNodes.size() > 1) {
                            pruneGraph(fEdges.filterBySet(cNodes), pmiMap, depth + 1);
                        }
                    }
                }
            } else {
                //logger.log(Level.INFO, "The component has size one ..");
            }
        }

        return aggregatedList;
    }

    public static <Node> DirectedGraph<Node> pruneMultipleParents(List<Node> nodes, DirectedGraph<Node> graph) {

        DirectedGraph<Node> cleanedEdges = new DirectedGraph<Node>(nodes);

        for (Node node : nodes) {
            List<Edge> incoming = graph.getIncoming(node);

            if (incoming != null) {
                Edge incomingEdge = findMaxWeightEdge(incoming);
                if ((incomingEdge != null)
                    && (incomingEdge.getFrom() != incomingEdge.getTo())) {
                    cleanedEdges.addEdge(graph.getNode(incomingEdge.getFrom()),
                            graph.getNode(incomingEdge.getTo()),
                            incomingEdge.getWeight());
                }
            }

            List<Edge> outgoing = graph.getOutgoing(node);
            if (outgoing != null) {
                for (Edge outgoingEdge : outgoing) {
                    if (outgoingEdge.getFrom() != outgoingEdge.getTo()) {
                        cleanedEdges.addEdge(graph.getNode(outgoingEdge.getFrom()),
                            graph.getNode(outgoingEdge.getTo()),
                            outgoingEdge.getWeight());
                    }
                }
            }
        }

        // TODO remove parallel edges
        return cleanedEdges;
    }

    public static <Node> Set<Set<Node>> connectedSets(DirectedGraph<Node> graph) {
        final Map<Node, Set<Node>> sets = new HashMap<>();
        for (Node n : graph.nodes) {
            Set<Node> s = new TreeSet<>();
            s.add(n);
            sets.put(n, s);
        }
        for (Edge e : graph.getEdges()) {
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

    private static <Node> DirectedGraph<Node> filterEdges(DirectedGraph<Node> graph, Set<Node> nodes) {

        DirectedGraph<Node> filteredEdges = new DirectedGraph<>(new ArrayList<>(nodes));

        for (Edge edge : graph.getEdges()) {
            Node from = graph.getNode(edge.getFrom());
            Node to = graph.getNode(edge.getTo());
            if (nodes.contains(from) && nodes.contains(to)) {
                filteredEdges.addEdge(from, to, edge.getWeight());
            }
        }
        return filteredEdges;
    }

    private static <Node> Node findRootName(Map<Node, Double> pmiMap, Collection<Node> nodes) {

        double maxSumPMI = Double.NEGATIVE_INFINITY;
        Node root = null;

        for (Node nodei : nodes) {

            if (pmiMap.get(nodei) > maxSumPMI) {
                maxSumPMI = pmiMap.get(nodei);
                root = nodei;
            }
        }

        if (root == null) {
            root = nodes.iterator().next();
        }

        return root;
    }

    private static <Node> DirectedGraph<Node> disconnectFalseRoots(DirectedGraph<Node> graph, Node root) {
        Collection<Edge> edges = graph.getEdges();
        final Object2IntMap<Node> incomingLinks = new Object2IntOpenHashMap<>();

        DirectedGraph<Node> cleanedList = new DirectedGraph<>(graph.getNodes());

        // compute the number of incoming links for each node
        for (Edge edge : edges) {
            Node dest = graph.getNode(edge.getTo());
            incomingLinks.put(dest, incomingLinks.getInt(dest) + 1);
        }

        for (Edge edge : edges) {
            Node nodeFrom = graph.getNode(edge.getFrom());

            if (nodeFrom != null && incomingLinks.containsKey(nodeFrom) && (nodeFrom.equals(root) || incomingLinks.get(nodeFrom) > 0)) {
                cleanedList.addEdge(edge, graph);
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
