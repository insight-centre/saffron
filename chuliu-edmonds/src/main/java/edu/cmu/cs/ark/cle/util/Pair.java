package edu.cmu.cs.ark.cle.util;

import com.google.common.base.Objects;

/**
 * 2-tuple of anything!
 *
 * @author sthomson@cs.cmu.edu
 */
public class Pair<T,V> {
	public final T first;
	public final V second;

	public Pair(T a, V b) {
		this.first = a;
		this.second = b;
	}

	/** Convenience static constructor */
	public static <T, V> Pair<T, V> of(T a, V b) {
		return new Pair<T, V>(a, b);
	}

	@Override public boolean equals(Object other) {
		if (!(other instanceof Pair)) return false;
		final Pair wOther = (Pair) other;
		return Objects.equal(first, wOther.first) && Objects.equal(second, wOther.second);
	}

	@Override public int hashCode() {
		return Objects.hashCode(first, second);
	}

	@Override public String toString() {
		return Objects.toStringHelper(this)
				.add("first", first)
				.add("second", second).toString();
	}
}
