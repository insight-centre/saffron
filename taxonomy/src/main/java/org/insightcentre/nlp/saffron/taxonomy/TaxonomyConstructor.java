package org.insightcentre.nlp.saffron.taxonomy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.taxonomy.graph.DirectedGraph;
import org.insightcentre.nlp.saffron.taxonomy.graph.DirectedGraph.Edge;
import org.insightcentre.nlp.saffron.taxonomy.graph.GraphPruning;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TaxonomyConstructor {

    public static DirectedGraph<Topic> optimisedSimilarityGraph(DocumentSearcher sd,
        double simThreshold, int spanSize, Map<String, Topic> topics, int minCommonDocs) throws SearchException {

        List<Topic> nodes = new ArrayList<>(topics.values());

        DirectedGraph<Topic> tmpGraph = constructEdgesWithoutDirections(sd, nodes, simThreshold,
            minCommonDocs);

        if (tmpGraph.getEdges().size() > 0) {
            // Map of topic string -> (undirected) edges
            Map<String, List<Edge>> organisedEdges = organiseEdges(tmpGraph);

            System.err.println("Compute SumPMI for all the topics based on existing edges");

			//if (useCachedPMI && pmiFile.exists()) {
            //	sumPMIMap = readSumPMIMap(pmiFile);
            //} else {
            Map<Topic, Double> sumPMIMap = computeSumPMI(sd, topics, organisedEdges, tmpGraph, spanSize);
            //}

            System.err.println("Construct directed edges based on SumPMI");
            List<Edge> edges = constructDirectedEdges(simThreshold, tmpGraph, sumPMIMap);

            edges = normaliseEdgeWeights(edges);

            System.err.println("Prune the graph");
            DirectedGraph<Topic> prunedEdges = GraphPruning.pruneGraph(new DirectedGraph(nodes, edges), sumPMIMap);

			// Remove edges for nodes that have more than one parent,
            // parallel
            // edges and self referring edges
            prunedEdges = GraphPruning.pruneMultipleParents(nodes, prunedEdges);

            return prunedEdges;
			//DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> jgrapht = JGraphTRepresentation
            //		.convertToJGraphT(nodes, prunedEdges);

			//System.err.println("Export graph to DOT");
            //JGraphTRepresentation.exportToDot(db, jgrapht, prunedGraphFile.getAbsolutePath(), sumPMIMap);
			//System.err.println("Printing taxonomy tree");
            //System.out.println(JGraphTRepresentation.taxonomyTreeText(jgrapht, sumPMIMap));
        } else {
            System.err.println("No edges found, try relaxing the thresholds for similarity.");
            return new DirectedGraph<>();
        }
    }

    public static List<Edge> normaliseEdgeWeights(List<Edge> edges) {

        List<Edge> normEdges = new ArrayList<>();
        double minWeight = edges.get(0).getWeight();
        double maxWeight = edges.get(0).getWeight();
        for (Edge edge : edges) {
            double weight = edge.getWeight();

            if (weight < minWeight) {
                minWeight = weight;
            }

            if ((!Double.isInfinite(weight)) && (weight > maxWeight)) {
                maxWeight = weight;
            }
        }

        minWeight = Math.log(1 + minWeight);
        maxWeight = Math.log(1 + maxWeight);

        for (Edge edge : edges) {
            double w = edge.getWeight();

            if (Double.isInfinite(w)) {
                w = maxWeight;
            } else {
                w = Math.log(1 + edge.getWeight());
            }

            double normWeight = 10000 * (w - minWeight) / (maxWeight - minWeight);
            int intWeight = (int)normWeight / 10;
            normEdges.add(new Edge(edge.getFrom(), edge.getTo(), intWeight));
        }

        return normEdges;
    }
    
	public static DirectedGraph<Topic> constructEdgesWithoutDirections(DocumentSearcher sd, List<Topic> nodes,
			Double simThreshold, int minCommonDocs) throws SearchException {
		// BARRY: First get occurrence map (doc -> occ) for all topics.
		List<Edge> edgeList = new ArrayList<>();

		Map<Topic, Map<String, Integer>> occMaps = new HashMap<>();

		for (Topic t : nodes) {
				Map<String, Integer> occMap = new HashMap<>();

				List<MorphologicalVariation> mvList = t.getMvList();
				for (MorphologicalVariation mv : mvList) {
					String searchString = mv.getString().replace("-", " ");
					Map<String, Integer> occMapTmp = sd.searchOccurrence(searchString, sd.numDocs());

					Set<String> occMapTmpSet = occMapTmp.keySet();
					for (String doc : occMapTmpSet) {
						if (occMap.containsKey(doc)) {

							int oldValue = occMap.get(doc);
							occMap.put(doc, occMapTmp.get(doc) + oldValue);
						} else {
							occMap.put(doc, occMapTmp.get(doc));
						}
					}
				}

				occMaps.put(t, occMap);
		}

		/*
		 * BARRY: - If one topic string ends with another topic string make
		 * undirected edge between them with weight 0.5 - else find num docs
		 * they both occur in: - if greater than minCommonDocs edge weight is
		 * |cooc|/(1+paper_count1+paper_count2) if that formula > simThreshold.
		 */
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = 0; j < nodes.size(); j++) {
				// compute only for the triangular matrix
				if ((nodes.get(i) != nodes.get(j)) && (i >= j)) {

					String topic1ID = nodes.get(i).getPreferredString();
					String topic2ID = nodes.get(j).getPreferredString();

					// Add substring edges with maximum weight
					if (topic1ID.endsWith(" " + topic2ID) || topic2ID.endsWith(" " + topic1ID)
					// || topic2ID.startsWith(topic1ID + " ")
					// || topic1ID.startsWith(topic2ID + " ")
					// || topic2ID.contains(" " + topic1ID + " ")
					// || topic1ID.contains(" " + topic2ID + " ")
					) {
						edgeList.add(new Edge(i, j, 0.5));
					} else {

						// compute similarity based on co-occurrence
						Map<String, Integer> occMap1 = occMaps.get(nodes.get(i));
						Map<String, Integer> occMap2 = occMaps.get(nodes.get(j));

						// check first if at least a minimum number of documents
						// mention
						// them
						// together
						int cooc = computeCoocurrence(occMap1, occMap2, sd.numDocs());
						if (cooc >= minCommonDocs) {

							int t1Docs = occMap1.size();
							int t2Docs = occMap2.size();

							// TODO in previous experiments we used t1Docs +
							// t2Docs
							double weight = (double)cooc / (1 + t1Docs + t2Docs);
							// Double weight = new Double(cooc) / (1 + t1Docs *
							// t2Docs);

							if (weight > simThreshold) {
								// weight =
								// weight
								// * weight
								// * (1 / (pmiMap.get(nodei.getTopicString()) -
								// pmiMap
								// .get(nodej.getTopicString())));

								edgeList.add(new Edge(i, j, weight));
							}
						}
					}
				}
			}
		}

		return new DirectedGraph(nodes, edgeList);
	}

	private static Map<String, List<Edge>> organiseEdges(DirectedGraph<Topic> tmpGraph) {

		Map<String, List<Edge>> organisedEdges = new HashMap<>();

		for (Edge tmpEdge : tmpGraph.getEdges()) {
			String node1 = tmpGraph.getNode(tmpEdge.getFrom()).getPreferredString();
			String node2 = tmpGraph.getNode(tmpEdge.getTo()).getPreferredString();

			if (!organisedEdges.containsKey(node1) && !organisedEdges.containsKey(node2)) {
				List<Edge> edges = new ArrayList<>();
				edges.add(tmpEdge);
				organisedEdges.put(node1, edges);
				organisedEdges.put(node2, edges);
			} else if (!organisedEdges.containsKey(node1)) {
				List<Edge> edges = new ArrayList<>();
				edges.add(tmpEdge);
				organisedEdges.put(node1, edges);
				List<Edge> edges2 = organisedEdges.get(node2);
				edges2.add(tmpEdge);
				organisedEdges.put(node2, edges2);
			} else if (!organisedEdges.containsKey(node2)) {
				List<Edge> edges = new ArrayList<>();
				edges.add(tmpEdge);
				organisedEdges.put(node2, edges);
				List<Edge> edges1 = organisedEdges.get(node1);
				edges1.add(tmpEdge);
				organisedEdges.put(node1, edges1);
			} else {
				List<Edge> edges1 = organisedEdges.get(node1);
				List<Edge> edges2 = organisedEdges.get(node2);
				edges1.add(tmpEdge);
				edges2.add(tmpEdge);
				organisedEdges.put(node1, edges1);
				organisedEdges.put(node2, edges2);
			}
		}

		return organisedEdges;
	}
    
    public static final int TOTAL_TOKENS_NO = 1000000;
    
	private static Map<Topic, Double> computeSumPMI(DocumentSearcher sd, Map<String, Topic> topics,
			Map<String, List<Edge>> organisedEdges, DirectedGraph graph, int spanSlop) throws SearchException {

		Map<Topic, Double> pmiMap = new HashMap<>();

		try {

			for (Topic topic : topics.values()) {

				System.err.println("Computing sum PMI for topic " + topic);
				double pmi = sumPMI(sd, topic,  organisedEdges, 
                    graph, TOTAL_TOKENS_NO, spanSlop);

				pmiMap.put(topic, pmi);
			}
		} finally {
		}

		// return MapUtils.sortByDoubleValue(pmiMap);
		return pmiMap;
	}

	public static double sumPMI(DocumentSearcher sd, Topic topic, 
			Map<String, List<Edge>> organisedEdges, DirectedGraph<Topic> graph, int totalTokensNo, int spanSlop) throws SearchException  {

		double contextWordsRank = 0.0;

		List<Edge> edges = organisedEdges.get(topic.getPreferredString());

		if (edges != null) {
			for (Edge edge : edges) {

				Topic otherTopic;
                Topic node1 = graph.getNode(edge.getFrom());
                Topic node2 = graph.getNode(edge.getTo());
                if(node1.equals(topic)) {
					otherTopic = node2;
				} else {
					otherTopic = node1;
				}

				// TODO use all MV strings not just the preferred string
				String searchString = otherTopic.getPreferredString().replace("-", " ");
				String searchString1 = topic.getPreferredString().replace("-", " ");

                final long spanFreq;
//				if (tKP != null) {
//					String searchString2 = tKP.getPreferredString().replace("-", " ");
//					spanFreq = sd.spanOccurrence(searchString1, searchString2, spanSlop, docsCount);
//				} else {
					spanFreq = sd.spanOccurrence(searchString1, searchString, spanSlop, sd.numDocs());
//				}

				double pxy = (double) spanFreq / totalTokensNo;
				double px = (double) otherTopic.getOverallOccurrence() / totalTokensNo;
				double py = (double) topic.getOverallMatches() / totalTokensNo;

				if (spanFreq > 0) {
					contextWordsRank += Math.log(pxy / (px * py));
				}
			}
		}

		return contextWordsRank; // topics.size();
	}

	private static List<Edge> constructDirectedEdges(double simThreshold, DirectedGraph<Topic> tmpGraph,
			Map<Topic, Double> sumPMIMap) {
        DirectedGraph<Topic> graph = new DirectedGraph<>();
		for (Edge tmpEdge : tmpGraph.getEdges()) {

			double weight = tmpEdge.getWeight();
            Topic node1 = tmpGraph.getNode(tmpEdge.getFrom());
            Topic node2 = tmpGraph.getNode(tmpEdge.getTo());
            if(!(node1.equals(node2)) &&
                weight > simThreshold) {
				graph = addEdge(sumPMIMap, graph, tmpEdge.getWeight(),
					node1, node2);
			}
		}
		return graph.getEdges();
	}

    public static DirectedGraph<Topic> addEdge(Map<Topic, Double> pmiMap, DirectedGraph<Topic> edges,
			double weight, Topic nodei, Topic nodej) {

		double pmiI = pmiMap.get(nodei);
		double pmiJ = pmiMap.get(nodej);

		// Idea was to connect to nodes of similar generality, doesn't work
		// Double squareDiff = (pmiI - pmiJ) * (pmiI - pmiJ);
		// if (squareDiff != 0) {
		// weight = weight / squareDiff;
		// }

        if(!nodei.equals(nodej)) {

			if (// (nodej.getTopicString().contains(nodei.getTopicString()))
			(nodej.getPreferredString().endsWith(" " + nodei.getPreferredString())) || (pmiI > pmiJ)) {

				// if (ti.getOverallOccurrence() >
				// tj.getOverallOccurrence()) {

				edges.addEdge(nodei, nodej, weight);
			} else {
				edges.addEdge(nodej, nodei, weight);
			}
		}
		return edges;
	}

  public static int computeCoocurrence(Map<String, Integer> occMap1,
      Map<String, Integer> occMap2, int docsNo) {
    int cooc = 0;

      Set<String> keys = occMap1.keySet();
      for (String key : keys) {
        if (occMap2.containsKey(key)) {
          cooc++;
        }
      }

    return cooc;
  }

}
