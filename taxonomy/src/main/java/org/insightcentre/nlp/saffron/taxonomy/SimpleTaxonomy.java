package org.insightcentre.nlp.saffron.taxonomy;

import java.util.*;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.Taxonomy;

public class SimpleTaxonomy {

    public static Taxonomy optimisedSimilarityGraph(List<DocumentTopic> docTopics, Map<String, Topic> topics) {

        final Map<Topic, Map<String, Integer>> occMap = getTopicMap(docTopics, topics);

        int totalDocs = getTotalDocs(docTopics);

        final List<Edge> graph = makeCompleteGraph(occMap, totalDocs);

        final Topic rootTopic = findRootTopic(occMap);

        final WeightedGraph wg = getMinimumSpanningTree(new WeightedGraph(graph));

        return makeTaxonomy(wg, rootTopic, new HashSet<Topic>());

    }

    private static Taxonomy makeTaxonomy(WeightedGraph wg, Topic rootTopic, HashSet<Topic> visited) {
        List<Taxonomy> t2 = new ArrayList<>();
        visited.add(rootTopic);
        for (Edge e : wg.getEdges(rootTopic)) {
            Topic next = rootTopic.equals(e.to) ? e.from : e.to;
            if (!visited.contains(next)) {
                t2.add(makeTaxonomy(wg, next, visited));
            }
        }
        return new Taxonomy(rootTopic.topicString, t2);
    }

    private static Map<Topic, Map<String, Integer>> getTopicMap(List<DocumentTopic> dts, Map<String, Topic> topics) {
        Map<Topic, Map<String, Integer>> map = new HashMap<>();
        for (DocumentTopic dt : dts) {
            final Topic t = topics.get(dt.topic_string);
            if (!map.containsKey(t)) {
                map.put(t, new HashMap<String, Integer>());
            }
            map.get(t).put(dt.document_id, dt.occurrences);
        }
        return map;
    }

    private static int getTotalDocs(List<DocumentTopic> dts) {
        Set<String> set = new HashSet<>();
        for (DocumentTopic dt : dts) {
            set.add(dt.document_id);
        }
        return set.size();
    }

    private static List<Edge> makeCompleteGraph(Map<Topic, Map<String, Integer>> occMap, int totalDocs) {
        final List<Edge> edges = new ArrayList<>();
        for (Map.Entry<Topic, Map<String, Integer>> e1 : occMap.entrySet()) {
            Topic t1 = e1.getKey();
            Map<String, Integer> occ1 = e1.getValue();
            for (Map.Entry<Topic, Map<String, Integer>> e2 : occMap.entrySet()) {
                Topic t2 = e2.getKey();
                Map<String, Integer> occ2 = e2.getValue();

                double pxy = (double) computeCoocurrence(occ1, occ2) / totalDocs;
                double px = (double) occ1.size() / totalDocs;
                double py = (double) occ2.size() / totalDocs;

                if (pxy > 0) {
                    edges.add(new Edge(t1, t2, pxy * Math.log(pxy / px / py)));
                }
            }
        }
        return edges;
    }

    private static Topic findRootTopic(Map<Topic, Map<String, Integer>> occMap) {
        Topic bestTopic = null;
        int bestSize = -1;
        for (Map.Entry<Topic, Map<String, Integer>> e1 : occMap.entrySet()) {
            if (e1.getValue().size() > bestSize) {
                bestTopic = e1.getKey();
                bestSize = e1.getValue().size();
            }
        }
        return bestTopic;
    }

    private static int computeCoocurrence(Map<String, Integer> occMap1,
            Map<String, Integer> occMap2) {
        int cooc = 0;

        Set<String> keys = occMap1.keySet();
        for (String key : keys) {
            if (occMap2.containsKey(key)) {
                cooc++;
            }
        }

        return cooc;
    }

    public static class WeightedGraph {

        private final List<Edge> edgeList;
        private final Map<Topic, List<Edge>> edges;
        private final Set<Topic> nodes;

        public WeightedGraph(List<Edge> edges) {
            this.edgeList = edges;
            this.edges = new HashMap<>();
            this.nodes = new HashSet<>();
            for (Edge e : edges) {
                if (!this.edges.containsKey(e.to)) {
                    this.edges.put(e.to, new ArrayList<Edge>());
                }
                this.edges.get(e.to).add(e);
                if (!this.edges.containsKey(e.from)) {
                    this.edges.put(e.from, new ArrayList<Edge>());
                }
                this.edges.get(e.from).add(e);
                this.nodes.add(e.to);
                this.nodes.add(e.from);
            }
        }

        public Set<Topic> getVertices() {
            return nodes;
        }

        public Collection<Edge> getEdges(Topic t) {
            if (edges.containsKey(t)) {
                return Collections.unmodifiableList(edges.get(t));
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public Collection<Edge> getEdges() {
            return Collections.unmodifiableList(edgeList);
        }

    }

    public static class Edge implements Comparable<Edge> {

        public Topic to, from;
        public double cost;

        public Edge(Topic to, Topic from, double cost) {
            this.to = to;
            this.from = from;
            this.cost = cost;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Edge)) {
                return false;
            }
            Edge e2 = (Edge) o;
            return to.equals(e2.to) && from.equals(e2.from) && cost == e2.cost;
        }

        @Override
        public int compareTo(Edge e) {
            if (cost > e.cost) {
                return -1;
            } else if (cost < e.cost) {
                return +1;
            } else {
                int i1 = to.topicString.compareTo(e.to.topicString);
                if (i1 != 0) {
                    return i1;
                }
                return from.topicString.compareTo(e.from.topicString);
            }

        }

        @Override
        public String toString() {
            return "Edge{" + "to=" + to + ", from=" + from + ", cost=" + cost + '}';
        }
    }

    public static WeightedGraph getMinimumSpanningTree(WeightedGraph graph) {
        final Queue<Edge> edges = new PriorityQueue<>(graph.getEdges());
        final List<Edge> finalGraph = new ArrayList<>();
        WeightedGraph wg = new WeightedGraph(finalGraph);

        for (Edge edge : edges) {
            if (!makesCycle(wg, edge)) {
                System.err.println("Add edge: " + edge);
                finalGraph.add(edge);
                wg = new WeightedGraph(finalGraph);
            } else {
                System.err.println("Skip edge: " + edge);
            }
        }

        return new WeightedGraph(finalGraph);
    }

    private static boolean makesCycle(WeightedGraph graph, Edge edge) {
        Set<Topic> visited = new HashSet<Topic>();
        visited.add(edge.to);
        _makesCycle(visited, graph, edge.to);
        return visited.contains(edge.from);
    }

    private static void _makesCycle(Set<Topic> visited, WeightedGraph g, Topic target) {
        for (Edge e : g.getEdges(target)) {
            Topic next = target.equals(e.to) ? e.from : e.to;
            if (!visited.contains(next)) {
                visited.add(next);
                _makesCycle(visited, g, next);
            }
        }
    }
    /*public static WeightedGraph getMinimumSpanningTree(WeightedGraph graph, Topic start) {
        if (graph == null)
            throw (new NullPointerException("Graph must be non-NULL."));

        double cost = 0;

        final Set<Topic> unvisited = new HashSet<Topic>();
        unvisited.addAll(graph.getVertices());
        unvisited.remove(start); // O(1)

        final List<Edge> path = new ArrayList<>();
        final Queue<Edge> edgesAvailable = new PriorityQueue<>();

        Topic vertex = start;
        while (!unvisited.isEmpty()) {
            // Add all edges to unvisited vertices
            for (Edge e : graph.getEdges(vertex)) {
                if (unvisited.contains(e.to))
                    edgesAvailable.add(e);
            }

            if(edgesAvailable.isEmpty())
                break;

            // Remove the lowest cost edge
            final Edge e = edgesAvailable.remove();
            cost += e.cost;
            path.add(e); // O(1)

            vertex = e.to;
            unvisited.remove(vertex); // O(1)
        }

        return new WeightedGraph(path);
    }*/
}
