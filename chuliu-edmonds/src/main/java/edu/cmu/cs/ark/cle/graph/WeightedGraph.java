package edu.cmu.cs.ark.cle.graph;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import edu.cmu.cs.ark.cle.util.Weighted;

import java.util.Collection;
import java.util.List;

/**
 * @author sthomson@cs.cmu.edu
 */
public abstract class WeightedGraph<V> {
	public abstract Collection<V> getNodes();

	public abstract double getWeightOf(V source, V dest);

	public abstract Collection<Weighted<Edge<V>>> getIncomingEdges(V destinationNode);

	public WeightedGraph<V> filterEdges(Predicate<Edge<V>> predicate) {
		final List<Weighted<Edge<V>>> allEdges = Lists.newArrayList();
		for (V node: getNodes()) {
			final Collection<Weighted<Edge<V>>> incomingEdges = getIncomingEdges(node);
			for (Weighted<Edge<V>> edge : incomingEdges) {
				if (predicate.apply(edge.val)) {
					allEdges.add(edge);
				}
			}
		}
		return SparseWeightedGraph.from(getNodes(), allEdges);
	}
}
