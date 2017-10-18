package edu.cmu.cs.ark.cle.ds;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;

/** A PriorityQueue built on top of a FibonacciHeap */
public class FibonacciQueue<E> extends AbstractQueue<E> {
	private final FibonacciHeap<E,E> heap;
	private final Function<FibonacciHeap<E,?>.Entry, E> getValue = new Function<FibonacciHeap<E, ?>.Entry, E>() {
		@Override public E apply(FibonacciHeap<E, ?>.Entry input) { return input.value; }
	};

	private FibonacciQueue(FibonacciHeap<E,E> heap) {
		this.heap = heap;
	}

	public static <C> FibonacciQueue<C> create(Comparator<? super C> comparator) {
		return new FibonacciQueue<C>(FibonacciHeap.<C,C>create(comparator));
	}

	public static <C extends Comparable> FibonacciQueue<C> create() {
		return new FibonacciQueue<C>(FibonacciHeap.<C,C>create());
	}

	public Comparator<? super E> comparator() {
		return heap.comparator();
	}

	@Override
	public E peek() {
		Optional<FibonacciHeap<E,E>.Entry> first = heap.peekOption();
		return first.isPresent() ? first.get().value : null;
	}

	@Override
	public boolean offer(E e) {
		return heap.add(e, e).isPresent();
	}

	@Override
	public E poll() {
		return heap.pollOption().orNull();
	}

	@Override
	public int size() {
        return heap.size();
    }

	@Override
	public Iterator<E> iterator() {
		//return Iterators.transform(heap.iterator(), getValue);
            final Iterator<FibonacciHeap<E,E>.Entry> iterator = heap.iterator();
                return new Iterator<E>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public E next() {
                        return iterator.next().value;
                    }
                };
	}
}
