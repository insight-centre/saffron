package org.insightcentre.nlp.saffron.taxonomy;

import ie.deri.unlp.javaservices.documentindex.DocumentSearcher;
import ie.deri.unlp.javaservices.documentindex.SearchException;
import org.insightcentre.nlp.saffron.taxonomy.db.Topic;
import org.insightcentre.nlp.saffron.taxonomy.db.TopicSimilarity;
import org.insightcentre.nlp.saffron.taxonomy.graph.Edge;
import org.insightcentre.nlp.saffron.taxonomy.graph.Edmonds;
import org.insightcentre.nlp.saffron.taxonomy.graph.GraphPruning;
import org.insightcentre.nlp.saffron.taxonomy.graph.JGraphTRepresentation;
import org.insightcentre.nlp.saffron.taxonomy.graph.Node;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Main {

  private static final Integer DOC_COUNT_SIMILARITY = 1;

  static Logger logger = Logger.getLogger(Main.class.getName());

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    final Integer top = 4000;
    logger.log(Level.INFO, "Constructing subsumption graph for top " + top
        + " topics..");

    // simpleTest();
    // CLEF 0 0.05
    //similarityGraph(top, 0.0, 1, true, 5, 0);

    // ACL 0.0, 5, 3 - for span 50 word sense disambiguation is the root
    // SW 0.0, 50, 1
    // UvT 0.0, 50, 1
    // WebSci 0.0, 5, 3
    // ClefCiting 0.0, 10, 2
    // Toine 0.0. 5, 3 
    constructGraphWithTopDBTopics(top, 0.0, 5, 3, false);
    // subsumptionGraph(top, 0.05);
  }

  private static void constructGraphWithTopDBTopics(Integer top,
      Double simThreshold, Integer spanSize, Integer minCommonDocs, Boolean useCachedPMI) throws SearchException, SQLException, IOException {
	 
	List<String> topics = App.db.topRankingTopicStrings(top);
    TaxonomyConstructor.optimisedSimilarityGraph(null, new File("./"), 
    		simThreshold, spanSize, //TODO: DocumentSearcher
    		topics, minCommonDocs, useCachedPMI);
  }

  /**
   * Use topic similarities for edge weights, direction given by Sum PMI
 * @throws SearchException 
 * @throws SQLException 
 * @throws IOException 
   */
  private static void similarityGraph(DocumentSearcher sd, Integer nodesNumber, Double simThreshold,
      Integer simMethod, Boolean useTermsForSumPMI, Integer spanSize,
      Integer minCommonDocs) throws SearchException, SQLException, IOException {

      logger.log(Level.INFO, "Constructing the nodes..");

      /*
       * logger.log(Level.INFO, "Computing all PMI values"); Map<String,
       * Map<String, Double>> pmiMaps = Subsumption.computePMIMaps(topics,
       * spanSize);
       * 
       * logger.log(Level.INFO, "Computing SumPMI for each topic"); Map<String,
       * Double> sumPMIMap = Subsumption.computeSumPMIOptimised(topics,
       * pmiMaps);
       * 
       * logger.log(Level.INFO, "Construct edges based on similarity");
       * AdjacencyList edges = constructSimEdgesOptimised(nodes, sumPMIMap,
       * simThreshold, pmiMaps);
       */

      logger.log(Level.INFO, "Computing SumPMI for each topic");

      Map<String, Double> sumPMIMap =
          Subsumption.computeSumPMI(sd, App.db.topRankingTopicStrings(nodesNumber),
              useTermsForSumPMI, "pmi.txt");
      // readSumPMIMap();
      logger.log(Level.INFO, "Construct edges based on similarity");
      TaxonomyConstructor tc = new TaxonomyConstructor();
      List<Node> nodes =
          tc.convertTopicsToNodes(App.db.topRankingTopicStrings(nodesNumber));
      // List<Node> nodes =
      // TaxonomyConstructor.convertTopicsToNodes(new ArrayList<String>(
      // sumPMIMap.keySet()));

      AdjacencyList edges =
          constructSimEdges(sd, nodes, sumPMIMap, simThreshold, simMethod,
              minCommonDocs);

      EMReportManager emrm = new EMReportManager();
      edges = emrm.normaliseEdgeWeights(edges);

      JGraphTRepresentation.exportToDot(
          JGraphTRepresentation.convertToJGraphT(nodes, edges),
          "similarityGraph.dot", sumPMIMap);

      logger.log(Level.INFO, "Prune the graph");
      AdjacencyList prunedEdges =
          GraphPruning.pruneGraph(nodes, edges, sumPMIMap);

      logger.log(Level.INFO, "Export graph to DOT");
      JGraphTRepresentation.exportToDot(
          JGraphTRepresentation.convertToJGraphT(nodes, prunedEdges),
          "prunedGraph.dot", sumPMIMap);
  }

  /**
   * Use topic subsumption for edges, direction and weight
 * @throws SearchException 
 * @throws SQLException 
 * @throws IOException 
   */
  private static void subsumptionGraph(DocumentSearcher sd, Integer nodesNumber, Double simThreshold) throws SearchException, IOException, SQLException {

    List<String> topics = new ArrayList<String>();
      topics = App.db.topRankingTopicStrings(nodesNumber);

      logger.log(Level.INFO, "Constructing the nodes..");
      TaxonomyConstructor tc = new TaxonomyConstructor();
      List<Node> nodes = tc.convertTopicsToNodes(topics);

      logger.log(Level.INFO, "Computing SumPMI for each topic");
      Map<String, Double> pmiMap =
          Subsumption.computeSumPMI(sd, topics, true, "pmi.txt");

      logger.log(Level.INFO, "Constructing edges based on subsumption..");
      // AdjacencyList edges =
      // constructSubsumpSimEdges(nodes, pmiMap, simThreshold);
      AdjacencyList edges = constructSubsumpEdges(sd, nodes, pmiMap);

      JGraphTRepresentation.exportToDot(
          JGraphTRepresentation.convertToJGraphT(nodes, edges),
          "subsumpGraph.dot", pmiMap);

      logger.log(Level.INFO, "Pruning graph..");
      AdjacencyList prunedEdges = GraphPruning.pruneGraph(nodes, edges, pmiMap);
      JGraphTRepresentation.exportToDot(
          JGraphTRepresentation.convertToJGraphT(nodes, prunedEdges),
          "prunedGraph.dot", pmiMap);
  }

  private static AdjacencyList constructSimEdgesOptimised(List<Node> nodes,
      Map<String, Double> sumPMIMap, Double simThreshold,
      Map<String, Map<String, Double>> pmiMaps) throws SQLException {

    AdjacencyList edges = new AdjacencyList();
    Double weight = null;

    for (Node nodei : nodes) {
      Topic t1 = App.db.getTopic(nodei.getTopicString());
      for (Node nodej : nodes) {
        Topic t2 = App.db.getTopic(nodej.getTopicString());
        if (nodei != nodej) {

          weight =
              pmiMaps.get(nodei.getTopicString()).get(nodej.getTopicString());

          if ((weight != null) && (weight > simThreshold)) {

            weight =
                weight * weight * Math.log(1 + t1.getRank() * t2.getRank());
            // weight =
            // weight
            // * weight
            // * (1 / (sumPMIMap.get(nodei.getTopicString()) - sumPMIMap
            // .get(nodej.getTopicString())));
            edges =
                TaxonomyConstructor.addEdge(sumPMIMap, edges, weight, nodei,
                    nodej);
          }
        }
      }
    }

    return edges;
  }

  private static AdjacencyList constructSimEdges(DocumentSearcher sd, List<Node> nodes,
      Map<String, Double> pmiMap, Double simThreshold, Integer simMethod,
      Integer minCommonDocs) throws SQLException, SearchException {
    AdjacencyList edges = new AdjacencyList();
    Double weight = null;
    Integer docsCount = App.db.numDocuments();

    Map<String, Map<String, Integer>> occMaps =
        new HashMap<String, Map<String, Integer>>();

    for (Node nodei : nodes) {
      Topic t = App.db.getTopic(nodei.getTopicString());
        Map<String, Integer> occMap =
            sd.searchOccurrence(t.getPreferredString(), docsCount);

        occMaps.put(nodei.getTopicString(), occMap);
    }

    for (Node nodei : nodes) {
      for (Node nodej : nodes) {

        if ((nodei != nodej)
            && (pmiMap.containsKey(nodei.getTopicString()) && (pmiMap
                .containsKey(nodej.getTopicString())))) {

          switch (simMethod) {
          case 1:
            // compute similarity based on co-occurrence
            Topic t1 = App.db.getTopic(nodei.getTopicString());
            Topic t2 = App.db.getTopic(nodej.getTopicString());

            // TODO this parameter should not be hard coded
            // check first if at least a minimum number of documents mention
            // them together
            if (App.db.selectCountJointTopics(t1.getRootSequence(),
                t2.getRootSequence()) >= minCommonDocs) {

              Map<String, Integer> occMap1 =
                  occMaps.get(nodei.getTopicString());
              Map<String, Integer> occMap2 =
                  occMaps.get(nodej.getTopicString());

              Integer cooc =
                  Subsumption.computeCoocurrence(occMap1, occMap2, docsCount);

              Integer t1Docs = occMap1.size();
              Integer t2Docs = occMap2.size();

              // TODO in previous experiments we used t1Docs + t2Docs
              weight = new Double(cooc) / (1 + t1Docs * t2Docs);

              if (weight > simThreshold) {
                // weight =
                // weight
                // * weight
                // * (1 / (pmiMap.get(nodei.getTopicString()) - pmiMap
                // .get(nodej.getTopicString())));
                edges =
                    TaxonomyConstructor.addEdge(pmiMap, edges, weight, nodei,
                        nodej);
              }
            }
            break;
          default:
            // use topic similarity table
            TopicSimilarity ts =
                (TopicSimilarity) App.db.getTopicSimilarity(nodei.getTopicString(),
                    nodej.getTopicString());

            if (ts != null && ts.getSimilarityScore() > simThreshold) {
              // Topic ti = Topic.get(nodei.getTopicString());
              // Topic tj = Topic.get(nodej.getTopicString());

              Double dist =
                  pmiMap.get(nodei.getTopicString())
                      - pmiMap.get(nodej.getTopicString());

              weight =
              // ts.getSimilarityScore() * pmiMap.get(nodei.getTopicString())
              // * pmiMap.get(nodej.getTopicString());

                  // ts.getSimilarityScore() * ( 1/
                  // (pmiMap.get(nodei.getTopicString())
                  // - pmiMap.get(nodej.getTopicString())));

                  ts.getSimilarityScore() * ts.getSimilarityScore()
                  // penalise very distant concepts (one specific, one
                  // generic)
                      * (1 / (dist * dist));

              edges =
                  TaxonomyConstructor.addEdge(pmiMap, edges,
                      ts.getSimilarityScore(), nodei, nodej);
              break;
            }
          }
        }
      }
    }

    return edges;
  }

  private static AdjacencyList constructSubsumpEdges(DocumentSearcher sd, List<Node> nodes,
      Map<String, Double> pmiMap) throws SQLException, SearchException {
    AdjacencyList edges = new AdjacencyList();

    Integer docsCount = App.db.numDocuments();

    for (Node nodei : nodes) {
      for (Node nodej : nodes) {

        String t1 = nodei.getTopicString();
        String t2 = nodej.getTopicString();

        // penalise very distant concepts (one specific, one generic)
        Double weight =
            1 / (pmiMap.get(nodei.getTopicString()) - pmiMap.get(nodej
                .getTopicString()));

        if (!t1.equals(t2) && Subsumption.subsumes(sd, t1, t2, docsCount)) {

          logger.log(Level.INFO, "Adding edge " + nodei.getTopicString() + "->"
              + nodej.getTopicString());
          edges.addEdge(nodei, nodej, weight);
        }
      }
    }

    return edges;
  }

  private static AdjacencyList constructSubsumpSimEdges(DocumentSearcher sd, List<Node> nodes,
      Map<String, Double> pmiMap, Double simThreshold) throws SQLException, SearchException {
    AdjacencyList edges = new AdjacencyList();

    Integer docsCount = App.db.numDocuments();

    for (Node nodei : nodes) {
      for (Node nodej : nodes) {

        TopicSimilarity ts =
            (TopicSimilarity) App.db.getTopicSimilarity(nodei.getTopicString(),
                nodej.getTopicString());

        String t1 = nodei.getTopicString();
        String t2 = nodej.getTopicString();

        if ((t1 != t2) && (ts != null) && (ts.getSimilarityScore() > 0.05)) {

          Double weight =
              ts.getSimilarityScore()
                  * ts.getSimilarityScore()
                  * (1 / (pmiMap.get(nodei.getTopicString()) - pmiMap.get(nodej
                      .getTopicString())));

          if (Subsumption.subsumes(sd, t1, t2, docsCount)) {
            edges.addEdge(nodei, nodej, weight);
          }
        }
      }
    }

    return edges;
  }

  private static Node findRankedRootName(Map<String, Double> pmiMap,
      List<Node> nodes, AdjacencyList list) {

    Double maxRank = 0.0;
    Node root = null;
    Collection<Edge> edges = list.getAllEdges();
    Map<Integer, Integer> outgoingLinks = new HashMap<Integer, Integer>();

    for (Node node : nodes) {
      outgoingLinks.put(node.getName(), 0);
    }

    for (Edge edge : edges) {
      int source = edge.getFrom().getName();
      int value = outgoingLinks.get(source) + 1;
      outgoingLinks.remove(source);
      outgoingLinks.put(source, value);
    }

    for (Node nodei : nodes) {

      String key = nodei.getTopicString();
      Integer id = nodei.getName();
      Double rank = pmiMap.get(key) * outgoingLinks.get(id);
      if (rank > maxRank) {
        maxRank = pmiMap.get(key);
        root = nodei;
      }
    }

    return root;
  }

  private static AdjacencyList connectFalseRootsToRoot(AdjacencyList list,
      List<Node> nodes, Node root) {
    Collection<Edge> edges = list.getAllEdges();
    Map<Integer, Integer> incomingLinks = new HashMap<Integer, Integer>();

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

    for (Node node : nodes) {
      if ((node.getName() != root.getName())
          && incomingLinks.get(node.getName()) == 0) {
        logger.log(
            Level.INFO,
            "Connecting false root to root: " + root.getName() + " -> "
                + node.getName());
        list.addEdge(root, node, 0);
      }
    }

    return list;
  }

  private static void simpleTest() throws SQLException, IOException {

    List<Node> nodeList = new ArrayList<Node>();
    Node root = new Node(0, "ps0");
    nodeList.add(root);
    Node node1 = new Node(1, "ps1");
    nodeList.add(node1);
    Node node2 = new Node(2, "ps2");
    nodeList.add(node2);
    Node node3 = new Node(3, "ps3");
    nodeList.add(node3);
    Node node4 = new Node(4, "ps4");
    nodeList.add(node4);
    Node node7 = new Node(7, "ps7");
    nodeList.add(node7);
    Node node8 = new Node(8, "ps8");
    nodeList.add(node8);
    Node node9 = new Node(9, "ps9");
    nodeList.add(node9);

    AdjacencyList list = new AdjacencyList();
    list.addEdge(root, node1, 5);
    // list.addEdge(node1, root, 5);
    list.addEdge(root, node2, 5);
    list.addEdge(node1, node3, 5);
    list.addEdge(node1, node4, 5);
    list.addEdge(node1, node2, 2);
    list.addEdge(node3, node2, 2);
    list.addEdge(node2, node7, 2);
    list.addEdge(node7, node8, 2);
    list.addEdge(node7, node9, 2);

    Edmonds ed = new Edmonds();
    // AdjacencyList resultList = ed.getMaxBranching(root, list);
    // printGraph(resultList);

    JGraphTRepresentation.exportToDot(
        JGraphTRepresentation.convertToJGraphT(nodeList, list), "graph.dot",
        null);
  }

  private static void printGraph(AdjacencyList list) {
    Collection<Edge> edges = list.getAllEdges();

    for (Edge edge : edges) {
      System.out.println(edge.getFrom().getTopicString() + "("
          + edge.getFrom().getName() + ")" + " -> "
          + edge.getTo().getTopicString() + "(" + edge.getTo().getName() + ")");
    }
  }
}
