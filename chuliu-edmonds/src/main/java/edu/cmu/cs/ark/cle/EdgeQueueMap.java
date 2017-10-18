package edu.cmu.cs.ark.cle;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.cmu.cs.ark.cle.ds.FibonacciQueue;
import edu.cmu.cs.ark.cle.ds.Partition;
import edu.cmu.cs.ark.cle.graph.Edge;
import edu.cmu.cs.ark.cle.util.Pair;
import edu.cmu.cs.ark.cle.util.Weighted;
import java.util.ArrayList;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


class EdgeQueueMap<V> {
	final Partition<V> partition;
	public final Map<V, EdgeQueue<V>> queueByDestination;

	public static class EdgeQueue<V> {
		private final V component;
		public final FibonacciQueue<ExclusiveEdge<V>> edges;
		private final Partition<V> partition;

		private EdgeQueue(V component, Partition<V> partition) {
			this.component = component;
			this.edges = FibonacciQueue.create(Collections.reverseOrder()); // highest weight edges first
			this.partition = partition;
		}

		public static <T> EdgeQueue<T> create(T component, Partition<T> partition) {
			return new EdgeQueue<T>(component, partition);
		}

		public void addEdge(ExclusiveEdge<V> exclusiveEdge) {
			// only add if source is external to SCC
			if (partition.componentOf(exclusiveEdge.edge.source) == component) return;
			edges.add(exclusiveEdge);
		}

		public Optional<ExclusiveEdge<V>> popBestEdge() {
			return popBestEdge(Arborescence.<V>empty());
		}

		/** Always breaks ties in favor of edges in bestArborescence */
		public Optional<ExclusiveEdge<V>> popBestEdge(Arborescence<V> bestArborescence) {
			if (edges.isEmpty()) return Optional.absent();
			final LinkedList<ExclusiveEdge<V>> candidates = Lists.newLinkedList();
			do {
				candidates.addFirst(edges.poll());
			} while (!edges.isEmpty()
					&& edges.comparator().compare(candidates.getFirst(), edges.peek()) == 0  // has to be tied for best
					&& !bestArborescence.contains(candidates.getFirst().edge));   // break if we've found one in `bestArborescence`
			// at this point all edges in `candidates` have equal weight, and if one of them is in `bestArborescence`
			// it will be first
			final ExclusiveEdge<V> bestEdge = candidates.removeFirst();
//			edges.addAll(candidates); // add back all the edges we looked at but didn't pick
			for (ExclusiveEdge<V> c : candidates) {
				edges.add(c);
			}
			return Optional.of(bestEdge);
		}
	}

	EdgeQueueMap(Partition<V> partition) {
		this.partition = partition;
		this.queueByDestination = Maps.newHashMap();
	}

	public void addEdge(Weighted<Edge<V>> edge) {
		final V destination = partition.componentOf(edge.val.destination);
		if (!queueByDestination.containsKey(destination)) {
			queueByDestination.put(destination, EdgeQueue.create(destination, partition));
		}
		final List<Edge<V>> replaces = Lists.newLinkedList();
		queueByDestination.get(destination).addEdge(ExclusiveEdge.of(edge.val, replaces, edge.weight));
	}

	/** Always breaks ties in favor of edges in best */
	public Optional<ExclusiveEdge<V>> popBestEdge(V component, Arborescence<V> best) {
		if (!queueByDestination.containsKey(component)) return Optional.absent();
		return queueByDestination.get(component).popBestEdge(best);
	}

	public EdgeQueue merge(V component, Iterable<Pair<EdgeQueue<V>, Weighted<Edge<V>>>> queuesToMerge) {
		final EdgeQueue<V> result = EdgeQueue.create(component, partition);
                final List<Pair<EdgeQueue<V>, Weighted<Edge<V>>>> l = new ArrayList<>();
                for(Pair<EdgeQueue<V>, Weighted<Edge<V>>> queueAndReplace : queuesToMerge) {
                    l.add(queueAndReplace);
                }
		for (Pair<EdgeQueue<V>, Weighted<Edge<V>>> queueAndReplace : l) {
			final EdgeQueue<V> queue = queueAndReplace.first;
			final Weighted<Edge<V>> replace = queueAndReplace.second;
			for (ExclusiveEdge<V> wEdgeAndExcluded : queue.edges) {
				final List<Edge<V>> replaces = wEdgeAndExcluded.excluded;
				replaces.add(replace.val);
				result.addEdge(ExclusiveEdge.of(
						wEdgeAndExcluded.edge,
						replaces,
						wEdgeAndExcluded.weight - replace.weight));
			}
		}
		queueByDestination.put(component, result);
		return result;
	}
}
