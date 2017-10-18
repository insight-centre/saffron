package edu.cmu.cs.ark.cle.graph;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.cmu.cs.ark.cle.util.Weighted;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author sthomson@cs.cmu.edu
 */
public class SparseWeightedGraph<V> extends WeightedGraph<V> {
	final private Set<V> nodes;
	final private Map<V, Map<V, Weighted<Edge<V>>>> incomingEdges;

	private SparseWeightedGraph(Set<V> nodes, Map<V, Map<V, Weighted<Edge<V>>>> incomingEdges) {
		this.nodes = nodes;
		this.incomingEdges = incomingEdges;
	}

	public static <T> SparseWeightedGraph<T> from(Iterable<T> nodes, Iterable<Weighted<Edge<T>>> edges) {
		final Map<T, Map<T, Weighted<Edge<T>>>> incomingEdges = Maps.newHashMap();
		for (Weighted<Edge<T>> edge : edges) {
			if (!incomingEdges.containsKey(edge.val.destination)) {
				incomingEdges.put(edge.val.destination, Maps.<T, Weighted<Edge<T>>>newHashMap());
			}
			incomingEdges.get(edge.val.destination).put(edge.val.source, edge);
		}
		return new SparseWeightedGraph<T>(ImmutableSet.copyOf(nodes), incomingEdges);
	}

	public static <T> SparseWeightedGraph<T> from(Iterable<Weighted<Edge<T>>> edges) {
		final Set<T> nodes = Sets.newHashSet();
		for (Weighted<Edge<T>> edge : edges) {
			nodes.add(edge.val.source);
			nodes.add(edge.val.destination);
		}
		return SparseWeightedGraph.from(nodes, edges);
	}

	@Override
	public Collection<V> getNodes() {
		return nodes;
	}

	@Override
	public double getWeightOf(V source, V dest) {
		if (!incomingEdges.containsKey(dest)) return Double.NEGATIVE_INFINITY;
		final Map<V, Weighted<Edge<V>>> inEdges = incomingEdges.get(dest);
		if (!inEdges.containsKey(source)) return Double.NEGATIVE_INFINITY;
		return inEdges.get(source).weight;
	}

	@Override
	public Collection<Weighted<Edge<V>>> getIncomingEdges(V destinationNode) {
		if (!incomingEdges.containsKey(destinationNode)) return ImmutableSet.of();
		return incomingEdges.get(destinationNode).values();
	}
}
