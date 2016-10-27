package org.insightcentre.nlp.saffron.taxonomy;

import org.insightcentre.nlp.saffron.taxonomy.graph.AdjacencyList;
import ie.deri.unlp.javaservices.documentindex.DocumentSearcher;
import ie.deri.unlp.javaservices.documentindex.SearchException;
import org.insightcentre.nlp.saffron.taxonomy.db.MorphologicalVariation;
import org.insightcentre.nlp.saffron.taxonomy.db.Topic;
import org.insightcentre.nlp.saffron.taxonomy.graph.GraphPruning;
import org.insightcentre.nlp.saffron.taxonomy.graph.Node;
import org.insightcentre.nlp.saffron.taxonomy.graph.TmpEdge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.insightcentre.nlp.saffron.taxonomy.db.DAO;
import org.insightcentre.nlp.saffron.taxonomy.graph.DirectedGraph;

public class TaxonomyConstructor {

	/**
	 * Build first the graph and then compute node strength with SumPMI
	 * 
	 * @throws SearchException
	 * @throws SQLException 
	 * @throws IOException 
	 */

	public static DirectedGraph optimisedSimilarityGraph(DAO db, DocumentSearcher sd, 
			Double simThreshold, Integer spanSize, List<String> topics, Integer minCommonDocs,
			Boolean useCachedPMI) throws SearchException, SQLException, IOException {
		TaxonomyConstructor tc = new TaxonomyConstructor();

		System.err.println("Construct the nodes list");

		List<Node> nodes = tc.convertTopicsToNodes(topics);
		System.err.println("Found " + nodes.size() + " nodes");
		System.err.println( "Construct edges based on similarity");

		List<TmpEdge> tmpEdges = tc.constructEdgesWithoutDirections(db, sd, nodes, simThreshold,
				minCommonDocs);

		if (tmpEdges.size() > 0) {
			// Map of topic string -> (undirected) edges
			Map<String, List<TmpEdge>> organisedEdges = tc.organiseEdges(tmpEdges);

			System.err.println( "Compute SumPMI for all the topics based on existing edges");
			Map<String, Double> sumPMIMap = new HashMap<String, Double>();

			//if (useCachedPMI && pmiFile.exists()) {
			//	sumPMIMap = readSumPMIMap(pmiFile);
			//} else {
				sumPMIMap = tc.computeSumPMI(db, sd, topics, organisedEdges, spanSize);
			//}

			System.err.println( "Construct directed edges based on SumPMI");
			AdjacencyList edges = tc.constructDirectedEdges(simThreshold, tmpEdges, sumPMIMap);

			EMReportManager emrm = new EMReportManager();
			edges = emrm.normaliseEdgeWeights(edges);

			System.err.println( "Exporting graph to DOT format");
			//DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> jGraphTGraph = JGraphTRepresentation
			//		.convertToJGraphT(nodes, edges);
			//JGraphTRepresentation.exportToDot(db, jGraphTGraph, simGraphFile.getAbsolutePath(), sumPMIMap);

			/*
			 * // Use this when SumPMI and the similarity graph is available
			 * logger.log(Level.INFO,
			 * "Read the previously computed SumPMI values"); Map<String,
			 * Double> sumPMIMap = readSumPMIMap();
			 * 
			 * logger.log(Level.INFO, "Import the similarity graph");
			 * 
			 * //List<Node> loadedNodes =
			 * //SaffronDotImporter.importNodes("similarityGraph.dot");
			 * 
			 * logger.log(Level.INFO, "Reading the similarity graph..");
			 * AdjacencyList edges =
			 * SaffronDotImporter.importEdges("similarityGraph.dot", nodes);
			 */

			System.err.println("Prune the graph");
			AdjacencyList prunedEdges = GraphPruning.pruneGraph(nodes, edges, sumPMIMap);

			// Remove edges for nodes that have more than one parent,
			// parallel
			// edges and self referring edges
			prunedEdges = GraphPruning.pruneMultipleParents(nodes, prunedEdges);

            return new DirectedGraph(nodes, prunedEdges);
			//DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> jgrapht = JGraphTRepresentation
			//		.convertToJGraphT(nodes, prunedEdges);

			//System.err.println("Export graph to DOT");
			//JGraphTRepresentation.exportToDot(db, jgrapht, prunedGraphFile.getAbsolutePath(), sumPMIMap);

			//System.err.println("Printing taxonomy tree");
			//System.out.println(JGraphTRepresentation.taxonomyTreeText(jgrapht, sumPMIMap));

		} else {
			System.err.println("No edges found, try relaxing the thresholds for similarity.");
            return new DirectedGraph();
		}
	}

	private AdjacencyList constructDirectedEdges(Double simThreshold, List<TmpEdge> tmpEdges,
			Map<String, Double> sumPMIMap) {
		AdjacencyList directedEdges = new AdjacencyList();

		for (TmpEdge tmpEdge : tmpEdges) {

			Double weight = tmpEdge.getWeight();
			if ((tmpEdge.getNode1().getTopicString() != tmpEdge.getNode2().getTopicString())
					&& (tmpEdge.getNode1().getName() != tmpEdge.getNode2().getName())
					&& weight > simThreshold) {
				directedEdges = addEdge(sumPMIMap, directedEdges, tmpEdge.getWeight(),
						tmpEdge.getNode1(), tmpEdge.getNode2());
			}
		}
		return directedEdges;
	}

	private Map<String, List<TmpEdge>> organiseEdges(List<TmpEdge> tmpEdges) {

		Map<String, List<TmpEdge>> organisedEdges = new HashMap<String, List<TmpEdge>>();

		for (TmpEdge tmpEdge : tmpEdges) {
			String node1 = tmpEdge.getNode1().getTopicString();
			String node2 = tmpEdge.getNode2().getTopicString();

			if (!organisedEdges.containsKey(node1) && !organisedEdges.containsKey(node2)) {
				List<TmpEdge> edges = new ArrayList<TmpEdge>();
				edges.add(tmpEdge);
				organisedEdges.put(node1, edges);
				organisedEdges.put(node2, edges);
			} else if (!organisedEdges.containsKey(node1)) {
				List<TmpEdge> edges = new ArrayList<TmpEdge>();
				edges.add(tmpEdge);
				organisedEdges.put(node1, edges);
				List<TmpEdge> edges2 = organisedEdges.get(node2);
				edges2.add(tmpEdge);
				organisedEdges.put(node2, edges2);
			} else if (!organisedEdges.containsKey(node2)) {
				List<TmpEdge> edges = new ArrayList<TmpEdge>();
				edges.add(tmpEdge);
				organisedEdges.put(node2, edges);
				List<TmpEdge> edges1 = organisedEdges.get(node1);
				edges1.add(tmpEdge);
				organisedEdges.put(node1, edges1);
			} else {
				List<TmpEdge> edges1 = organisedEdges.get(node1);
				List<TmpEdge> edges2 = organisedEdges.get(node2);
				edges1.add(tmpEdge);
				edges2.add(tmpEdge);
				organisedEdges.put(node1, edges1);
				organisedEdges.put(node2, edges2);
			}
		}

		return organisedEdges;
	}

	public List<TmpEdge> constructEdgesWithoutDirections(DAO db, DocumentSearcher sd, List<Node> nodes,
			Double simThreshold, Integer minCommonDocs) throws SQLException, SearchException {
		// BARRY: First get occurrence map (doc -> occ) for all topics.
		List<TmpEdge> edgeList = new ArrayList<TmpEdge>();

		int docsCount = db.numDocuments();

		Map<String, Map<String, Integer>> occMaps = new HashMap<String, Map<String, Integer>>();

		for (Node nodei : nodes) {
			Topic t = db.getTopic(nodei.getTopicString());
				Map<String, Integer> occMap = new HashMap<String, Integer>();

				List<MorphologicalVariation> mvList = t.getMvList();
				for (MorphologicalVariation mv : mvList) {
					String searchString = mv.getString().replace("-", " ");
					Map<String, Integer> occMapTmp = sd.searchOccurrence(searchString, docsCount);

					Set<String> occMapTmpSet = occMapTmp.keySet();
					for (String doc : occMapTmpSet) {
						if (occMap.containsKey(doc)) {

							Integer oldValue = occMap.get(doc);
							occMap.put(doc, occMapTmp.get(doc) + oldValue);
						} else {
							occMap.put(doc, occMapTmp.get(doc));
						}
					}
				}

				occMaps.put(nodei.getTopicString(), occMap);
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

					String topic1ID = nodes.get(i).getTopicString();
					String topic2ID = nodes.get(j).getTopicString();

					// Add substring edges with maximum weight
					if (topic1ID.endsWith(" " + topic2ID) || topic2ID.endsWith(" " + topic1ID)
					// || topic2ID.startsWith(topic1ID + " ")
					// || topic1ID.startsWith(topic2ID + " ")
					// || topic2ID.contains(" " + topic1ID + " ")
					// || topic1ID.contains(" " + topic2ID + " ")
					) {
						edgeList.add(new TmpEdge(nodes.get(i), nodes.get(j), 0.5));
					} else {

						// compute similarity based on co-occurrence
						Map<String, Integer> occMap1 = occMaps.get(topic1ID);
						Map<String, Integer> occMap2 = occMaps.get(topic2ID);

						// check first if at least a minimum number of documents
						// mention
						// them
						// together
						Integer cooc = Subsumption.computeCoocurrence(occMap1, occMap2, docsCount);
						if (cooc >= minCommonDocs) {

							Integer t1Docs = occMap1.size();
							Integer t2Docs = occMap2.size();

							// TODO in previous experiments we used t1Docs +
							// t2Docs
							Double weight = new Double(cooc) / (1 + t1Docs + t2Docs);
							// Double weight = new Double(cooc) / (1 + t1Docs *
							// t2Docs);

							if (weight > simThreshold) {
								// weight =
								// weight
								// * weight
								// * (1 / (pmiMap.get(nodei.getTopicString()) -
								// pmiMap
								// .get(nodej.getTopicString())));

								edgeList.add(new TmpEdge(nodes.get(i), nodes.get(j), weight));
							}
						}
					}
				}
			}
		}

		return edgeList;
	}

	public List<Node> convertTopicsToNodes(List<String> topics) {
		List<Node> nodes = new ArrayList<Node>();
		int i = 0;
		for (String topic : topics) {
			Node node = new Node(i, topic);
			nodes.add(node);
			i++;
		}

		return nodes;
	}

	private Map<String, Double> computeSumPMI(DAO db, DocumentSearcher sd, List<String> topics,
			Map<String, List<TmpEdge>> organisedEdges, Integer spanSlop)
			throws IOException, SearchException, SQLException {

		Map<String, Long> topicsMap = new HashMap<String, Long>();

		Map<String, Double> pmiMap = new HashMap<String, Double>();

		try {

			Integer docsCount = db.numDocuments();

			// use all top topics
			for (String topic : topics) {
				Topic t = db.getTopic(topic);
				topicsMap.put(topic, new Long(t.getOverallOccurrence()));
			}

			for (String topic : topics) {

				System.err.println("Computing sum PMI for topic " + topic);
				Double pmi = sumPMI(db, sd, topic, topicsMap, organisedEdges, docsCount,
						db.calculateTotalTokensNo(), spanSlop);

				pmiMap.put(topic, pmi);
			}
		} finally {
		}

		// return MapUtils.sortByDoubleValue(pmiMap);
		return pmiMap;
	}

	public Double sumPMI(DAO db, DocumentSearcher sd, String topic, Map<String, Long> topics,
			Map<String, List<TmpEdge>> organisedEdges, Integer docsCount, Integer totalTokensNo,
			Integer spanSlop) throws SearchException, SQLException {

		Double contextWordsRank = 0.0;
		Topic t = db.getTopic(topic);

		List<TmpEdge> edges = organisedEdges.get(topic);

		if (edges != null) {
			for (TmpEdge edge : edges) {

				String otherTopic;
				if (edge.getNode1().getTopicString().equals(topic)) {
					otherTopic = edge.getNode2().getTopicString();
				} else {
					otherTopic = edge.getNode1().getTopicString();
				}

				Topic tKP = db.getTopic(otherTopic);

				Long spanFreq = new Long(0);
				// TODO use all MV strings not just the preferred string
				String searchString = otherTopic.replace("-", " ");
				String searchString1 = t.getPreferredString().replace("-", " ");

				if (tKP != null) {
					String searchString2 = tKP.getPreferredString().replace("-", " ");
					spanFreq = sd.spanOccurrence(searchString1, searchString2, spanSlop, docsCount);
				} else {
					spanFreq = sd.spanOccurrence(searchString1, searchString, spanSlop, docsCount);
				}

				double pxy = (double) spanFreq / totalTokensNo;
				double px = (double) topics.get(otherTopic) / totalTokensNo;
				double py = (double) t.getOverallMatches() / totalTokensNo;

				if (spanFreq > 0) {
					contextWordsRank += Math.log(pxy / (px * py));
				}
			}
		}

		return contextWordsRank; // topics.size();
	}

	/**
	 * @param pmiMap
	 * @param edges
	 * @param weight
	 * @param nodei
	 * @param nodej
	 */
	public static AdjacencyList addEdge(Map<String, Double> pmiMap, AdjacencyList edges,
			Double weight, Node nodei, Node nodej) {

		Double pmiI = pmiMap.get(nodei.getTopicString());
		Double pmiJ = pmiMap.get(nodej.getTopicString());

		// Idea was to connect to nodes of similar generality, doesn't work
		// Double squareDiff = (pmiI - pmiJ) * (pmiI - pmiJ);
		// if (squareDiff != 0) {
		// weight = weight / squareDiff;
		// }

		if (pmiI != null && pmiJ != null && nodei.getTopicString() != nodej.getTopicString()) {

			if (// (nodej.getTopicString().contains(nodei.getTopicString()))
			(nodej.getTopicString().endsWith(" " + nodei.getTopicString())) || (pmiI > pmiJ)) {

				// if (ti.getOverallOccurrence() >
				// tj.getOverallOccurrence()) {

				System.err.println(
						"Adding edge " + nodei.getTopicString() + "->" + nodej.getTopicString());
				edges.addEdge(nodei, nodej, weight);
			} else {
				System.err.println(
						"Adding edge " + nodej.getTopicString() + "->" + nodei.getTopicString());
				edges.addEdge(nodej, nodei, weight);
			}
		}
		return edges;
	}

	public static Map<String, Double> readSumPMIMap(File pmiFile) throws NumberFormatException,
			IOException {
		Map<String, Double> sumPMIMap = new HashMap<String, Double>();

		BufferedReader input = new BufferedReader(new FileReader(pmiFile));
		try {
			String line = null;
			while ((line = input.readLine()) != null) {
				Integer indexComma = line.indexOf(",");
				Double value = Double.parseDouble(line.substring(indexComma + 1));
				sumPMIMap.put(line.substring(0, indexComma), value);
			}
		} finally {
			input.close();
		}
		return sumPMIMap;
	}
}
