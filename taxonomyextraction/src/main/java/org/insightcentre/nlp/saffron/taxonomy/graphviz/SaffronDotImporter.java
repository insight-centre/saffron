package org.insightcentre.nlp.saffron.taxonomy.graphviz;

import org.insightcentre.nlp.saffron.taxonomy.AdjacencyList;
import org.insightcentre.nlp.saffron.taxonomy.graph.Node;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * This class is used to read a graph from a .dot file
 * 
 * @author Georgeta Bordea
 */
public class SaffronDotImporter {

	public static DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> importGraph(String path)
			throws IOException {

		DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> directedGraph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);

		BufferedReader input = new BufferedReader(new FileReader(path));
		try {
			String line = null;
			input.readLine();
			while ((line = input.readLine()) != null) {

				if (!line.contains("}")) {
					if (!line.contains("->")) {
						String nodeString = line.trim();
						nodeString = nodeString.substring(1, nodeString.length() - 2);
						directedGraph.addVertex(nodeString);
					} else {
						String edgeString = line.trim();
						String[] nodesWeight = edgeString.split(" -> ");

						String from = nodesWeight[0].substring(1, nodesWeight[0].length() - 1);
						String to = nodesWeight[1].substring(1, nodesWeight[1].lastIndexOf("\""));
						String weight = nodesWeight[1].substring(nodesWeight[1].indexOf("=") + 2,
								nodesWeight[1].length() - 2);

						// Ignore self referring edges
						if (from != to) {
							Double weightDouble = Double.parseDouble(weight);
							if (!directedGraph.containsVertex(from)) {
								directedGraph.addVertex(from);
							}
							if (!directedGraph.containsVertex(to)) {
								directedGraph.addVertex(to);
							}

							DefaultWeightedEdge e = directedGraph.addEdge(from, to);
							if (e != null) {
								directedGraph.setEdgeWeight(e, weightDouble);
							}
						}

					}
				}

			}
		} finally {
			input.close();
		}

		return directedGraph;
	}

	public static AdjacencyList importEdges(String path, List<Node> nodes) throws IOException {
		AdjacencyList edges = new AdjacencyList();

		BufferedReader input = new BufferedReader(new FileReader(path));
		try {
			String line = null;
			input.readLine();
			while ((line = input.readLine()) != null) {

				if (!line.contains("}")) {
					if (line.contains("->")) {
						String edgeString = line.trim();
						String[] nodesWeight = edgeString.split(" -> ");

						String from = nodesWeight[0].substring(1, nodesWeight[0].length() - 1);
						String to = nodesWeight[1].substring(1, nodesWeight[1].lastIndexOf("\""));

						if (from != to) {
							String weight = nodesWeight[1].substring(
									nodesWeight[1].indexOf("=") + 2, nodesWeight[1].length() - 2);
							int fromNodeId = -1;
							int toNodeId = -1;
							for (Node node : nodes) {
								if (node.getTopicString().equals(from)) {
									fromNodeId = node.getName();
								} else if (node.getTopicString().equals(to)) {
									toNodeId = node.getName();
								}
							}

							Double weightDouble = Double.parseDouble(weight);

							if (weightDouble != null && fromNodeId != -1 && toNodeId != -1) {
								edges.addEdge(new Node(fromNodeId, from), new Node(toNodeId, to),
										weightDouble);
							}
						}
					}
				}

			}
		} finally {
			input.close();
		}

		return edges;
	}

	public static List<Node> importNodes(String path) throws IOException {
		Map<String, Node> nodes = new HashMap<String, Node>();

		BufferedReader input = new BufferedReader(new FileReader(path));
		try {
			String line = null;
			input.readLine();

			Integer i = 0;
			while ((line = input.readLine()) != null) {

				if (!line.contains("}")) {
					if (line.contains("->")) {
						String edgeString = line.trim();
						String[] nodesWeight = edgeString.split(" -> ");

						String from = nodesWeight[0].substring(1, nodesWeight[0].length() - 1);
						String to = nodesWeight[1].substring(1, nodesWeight[1].lastIndexOf("\""));

						if (!nodes.containsKey(from)) {
							nodes.put(from, new Node(i, from));
							i++;
						}

						if (!nodes.containsKey(to)) {
							nodes.put(to, new Node(i, to));
							i++;
						}
					}
				}

			}
		} finally {
			input.close();
		}

		return new ArrayList<Node>(nodes.values());
	}
}
