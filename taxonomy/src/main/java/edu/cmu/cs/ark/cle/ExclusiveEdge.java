package edu.cmu.cs.ark.cle;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import edu.cmu.cs.ark.cle.graph.Edge;

import java.util.List;

/**
 * An edge, together with a list of edges that can't be in the final answer if 'edge' is.
 *
 * @author sthomson@cs.cmu.edu
 */
public class ExclusiveEdge<V> implements Comparable<ExclusiveEdge<V>> {
	public final Edge<V> edge;
	public final List<Edge<V>> excluded;
	public final double weight;

	private ExclusiveEdge(Edge<V> edge, List<Edge<V>> excluded, double weight) {
		this.edge = edge;
		this.excluded = excluded;
		this.weight = weight;
	}

	public static <T> ExclusiveEdge<T> of(Edge<T> edge, List<Edge<T>> excluded, double weight) {
		return new ExclusiveEdge<T>(edge, excluded, weight);
	}

	public static <T> ExclusiveEdge<T> of(Edge<T> edge, double weight) {
		return ExclusiveEdge.of(edge, ImmutableList.<Edge<T>>of(), weight);
	}

	@Override public int compareTo(ExclusiveEdge<V> exclusiveEdge) {
		return Doubles.compare(weight, exclusiveEdge.weight);
	}
}
