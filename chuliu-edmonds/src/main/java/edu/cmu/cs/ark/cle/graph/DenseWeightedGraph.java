package edu.cmu.cs.ark.cle.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.cmu.cs.ark.cle.util.Weighted;

import java.util.*;

import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.Range.closedOpen;

/**
 * @author sthomson@cs.cmu.edu
 */
public class DenseWeightedGraph<V> extends WeightedGraph<V> {
	final private ArrayList<V> nodes;
	final private Map<V, Integer> indexOf;
	final private double[][] weights;

	private DenseWeightedGraph(ArrayList<V> nodes, Map<V, Integer> indexOf, double[][] weights) {
		this.nodes = nodes;
		this.indexOf = indexOf;
		this.weights = weights;
	}

	public static <V> DenseWeightedGraph<V> from(Iterable<V> nodes, double[][] weights) {
		final ArrayList<V> nodeList = Lists.newArrayList(nodes);
		Preconditions.checkArgument(nodeList.size() == weights.length);
		final Map<V, Integer> indexOf = Maps.newHashMap();
		for (int i = 0; i < nodeList.size(); i++) {
			indexOf.put(nodeList.get(i), i);
		}
		return new DenseWeightedGraph<V>(nodeList, indexOf, weights);
	}

	public static DenseWeightedGraph<Integer> from(double[][] weights) {
		final Set<Integer> nodes = ContiguousSet.create(closedOpen(0, weights.length), integers());
		return DenseWeightedGraph.from(nodes, weights);
	}

	@Override
	public Collection<V> getNodes() {
		return nodes;
	}

	@Override
	public double getWeightOf(V source, V dest) {
		if (!indexOf.containsKey(source) || !indexOf.containsKey(dest)) return Double.NEGATIVE_INFINITY;
		return weights[indexOf.get(source)][indexOf.get(dest)];
	}

	@Override
	public Collection<Weighted<Edge<V>>> getIncomingEdges(V destinationNode) {
		if (!indexOf.containsKey(destinationNode)) return Collections.emptySet();
		final int dest = indexOf.get(destinationNode);
		List<Weighted<Edge<V>>> results = Lists.newArrayList();
		for (int src = 0; src < nodes.size(); src++) {
			results.add(Weighted.weighted(Edge.from(nodes.get(src)).to(destinationNode), weights[src][dest]));
		}
		return results;
	}
}
