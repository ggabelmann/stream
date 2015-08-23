package ggabelmann.stream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class is a very simplified version of {@link http://reactivex.io/RxJava/javadoc/rx/Observable.html}.
 * It provides a fluent builder for an Observable which can consume an Iterator or a Stream and emit events to Observers.
 * 
 * Note: the Stream used cannot have a terminal operation (because terminal operations consume Streams' items).
 * This class will consume all the items in the Stream by acting as its terminal operation.
 * 
 * @author Greg Gabelmann
 */
public class Observable<T> {
	
	private final Observer<T>[] observers;
	private final Stream<T> stream;
	
	private Observable(final Observer<T>[] observers, final Stream<T> stream) {
		this.observers = observers;
		this.stream = stream;
	}
	
	/**
	 * Consumes the Stream and makes calls to next() and/or error() and/or complete().
	 */
	public void emit() {
		try {
			stream.forEach((final T item) -> {
				for (final Observer<T> observer : observers) observer.next(item);
			});
		}
		catch (final Exception ex) {
			for (final Observer<T> observer : observers) observer.exception(ex);
			return;
		}
			
		for (final Observer<T> observer : observers) observer.complete();
	}
	
	/**
	 * @return A new builder for a new Observable.
	 */
	public static <T> FromStep<T> newBuilder() {
		return new Builder();
	}
	
	/**
	 * Represents a step in the build process.
	 */
	public static interface BuildStep<T> {
		/**
		 * @return A new Observable.
		 */
		public Observable<T> build();
	}
	
	/**
	 * Represents a step in the build process.
	 */
	public static interface FromStep<T> {
		/**
		 * Creates a Stream from the given iterator.
		 * The Stream will be ORDERED and not parallel.
		 * 
		 * @throws IllegalArgumentException If the given iterator is null.
		 */
		public SubscribeStep<T> from(final Iterator<T> iterator);
		
		/**
		 * @throws IllegalArgumentException If the given iterator is null.
		 */
		public SubscribeStep<T> from(final Stream<T> stream);
	}
	
	/**
	 * Represents a step in the build process.
	 */
	public static interface SubscribeStep<T> extends BuildStep<T> {
		/**
		 * Adds the given Observer as long as it does not equal any other Observers. (Uses equals().)
		 * @throws IllegalArgumentException If the given observer is null.
		 */
		public SubscribeStep<T> subscribe(final Observer<T> observer);
	}
	
	private static class Builder<T> implements BuildStep<T>, FromStep<T>, SubscribeStep<T> {
		
		private final Set<Observer<T>> observers = new HashSet<>();
		private Stream<T> stream = null;
		
		@Override
		public Observable<T> build() {
			return new Observable(observers.toArray(new Observer[observers.size()]), stream);
		}
		
		@Override
		public SubscribeStep<T> from(final Iterator<T> iterator) {
			if (iterator == null) throw new IllegalArgumentException();
			this.stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
			return this;
		}

		@Override
		public SubscribeStep<T> from(final Stream<T> stream) {
			if (stream == null) throw new IllegalArgumentException();
			this.stream = stream;
			return this;
		}

		@Override
		public SubscribeStep<T> subscribe(final Observer<T> observer) {
			if (observer == null) throw new IllegalArgumentException();
			this.observers.add(observer);
			return this;
		}

	}
	
}
