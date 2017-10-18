package edu.cmu.cs.ark.cle.ds;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Union-Find Data Structure
 *
 * @author sthomson@cs.cmu.edu
 */
public class Partition<V> {
	private final Map<V, V> parents;
	private final Map<V, Integer> ranks;

	private Partition(Map<V, V> parents, Map<V, Integer> ranks) {
		this.parents = parents;
		this.ranks = ranks;
	}

	/** Constructs a new partition of singletons */
	public static <T> Partition<T> singletons(Collection<T> nodes) {
		final Map<T, T> parents = Maps.newHashMap();
		final Map<T, Integer> ranks = Maps.newHashMap();
		for(T node : nodes) {
			parents.put(node, node); // each node is its own head
			ranks.put(node, 0); // every node has depth 0 to start
		}
		return new Partition<T>(parents, ranks);
	}

	/** Find the representative for the given item */
	public V componentOf(V i) {
		final V parent = parents.get(i);
		if (parent.equals(i)) {
			return i;
		} else {
			// collapse, so next lookup is O(1)
			parents.put(i, componentOf(parent));
		}
		return parents.get(i);
	}

	/** Merges the given components and returns the representative of the new component */
	public V merge(V a, V b) {
		final V aHead = componentOf(a);
		final V bHead = componentOf(b);
		if(aHead.equals(bHead)) return aHead;
		// add the shorter tree underneath the taller tree
		final int aRank = ranks.get(aHead);
		final int bRank = ranks.get(bHead);
		if (aRank > bRank) {
			parents.put(bHead, aHead);
			return aHead;
		} else if (bRank > aRank) {
			parents.put(aHead, bHead);
			return bHead;
		} else {
			// whoops, the tree got taller
			parents.put(bHead, aHead);
			ranks.put(aHead, aRank + 1);
			return aHead;
		}
	}

	/** Determines whether the two items are in the same component or not */
	public boolean sameComponent(V a, V b) {
		return componentOf(a) == componentOf(b);
	}

	public Set<V> getNodes() {
		return parents.keySet();
	}
}
