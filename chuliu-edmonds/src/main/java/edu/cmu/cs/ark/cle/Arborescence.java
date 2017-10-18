package edu.cmu.cs.ark.cle;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.cmu.cs.ark.cle.graph.Edge;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author sthomson@cs.cmu.edu
 */
public class Arborescence<V> {
	/**
	 * In an arborescence, each node (other than the root) has exactly one parent. This is the map
	 * from each node to its parent.
	 */
	public final ImmutableMap<V, V> parents;

	private Arborescence(ImmutableMap<V, V> parents) {
		this.parents = parents;
	}

	public static <T> Arborescence<T> of(ImmutableMap<T, T> parents) {
		return new Arborescence<T>(parents);
	}

	public static <T> Arborescence<T> empty() {
		return Arborescence.of(ImmutableMap.<T, T>of());
	}

	public boolean contains(Edge<V> e) {
		final V dest = e.destination;
		return parents.containsKey(dest) && parents.get(dest).equals(e.source);
	}

	@Override
	public String toString() {
		final List<String> lines = Lists.newArrayList();
		for (Map.Entry<V, V> entry : parents.entrySet()) {
			lines.add(entry.getValue() +  " -> " + entry.getKey());
		}
		return Objects.toStringHelper(this)
				.addValue(Joiner.on(", ").join(lines))
				.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		final Arborescence that = (Arborescence) other;
		Set<Map.Entry<V, V>> myEntries = parents.entrySet();
		Set thatEntries = that.parents.entrySet();
		return myEntries.size() == thatEntries.size() && myEntries.containsAll(thatEntries);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(parents);
	}
}
