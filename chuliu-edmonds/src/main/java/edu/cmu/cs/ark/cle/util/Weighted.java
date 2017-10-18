package edu.cmu.cs.ark.cle.util;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Add a weight to anything!
 *
 * @author sthomson@cs.cmu.edu
 */
public class Weighted<T> implements Comparable<Weighted<T>> {
	public final T val;
	public final double weight;

	public Weighted(T val, double weight) {
		checkNotNull(val);
		checkNotNull(weight);
		this.val = val;
		this.weight = weight;
	}

	/** Convenience static constructor */
	public static <T> Weighted<T> weighted(T value, double weight) {
		return new Weighted<T>(value, weight);
	}

	/** High weights first, use val.hashCode to break ties */
	public int compareTo(Weighted<T> other) {
		return ComparisonChain.start()
				.compare(other.weight, weight)
				.compare(Objects.hashCode(other.val), Objects.hashCode(val))
				.result();
	}

	@Override public boolean equals(Object other) {
		if (!(other instanceof Weighted)) return false;
		final Weighted wOther = (Weighted) other;
		return Objects.equal(val, wOther.val) && Objects.equal(weight, wOther.weight);
	}

	@Override public int hashCode() {
		return Objects.hashCode(val, weight);
	}

	@Override public String toString() {
		return Objects.toStringHelper(this)
				.add("val", val)
				.add("weight", weight).toString();
	}
}
