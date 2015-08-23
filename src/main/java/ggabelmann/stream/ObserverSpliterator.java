package ggabelmann.stream;

import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class is able to behave as an Observer, capture all events, and then emit them as a Spliterator.
 * It's possible to use a single thread to fire events, capture them, and then consume them in a stream, but
 * you have to be careful not to block on capturing the events or on tryAdvance().
 * 
 * @author Greg Gabelmann
 * @see ObservableTest
 */
public class ObserverSpliterator<T> implements Observer<T>, Spliterator<T> {
	
	private final BlockingQueue<Function<Consumer<? super T>, Boolean>> q;
	
	/**
	 * Constructor.
	 * Uses an unbounded BlockingQueue to hold items.
	 */
	public ObserverSpliterator() {
		this(new LinkedBlockingQueue<>());
	}
	
	/**
	 * Constructor.
	 * 
	 * @param queue A bounded BlockingQueue for holding items. It does not have to be empty.
	 * @throws IllegalArgumentException If the given queue is null.
	 */
	public ObserverSpliterator(final BlockingQueue<Function<Consumer<? super T>, Boolean>> queue) {
		if (queue == null) throw new IllegalArgumentException();
		this.q = queue;
	}
	
	/**
	 * {@inheritDoc}
	 * It may block if the internal queue is full.
	 */
	@Override
	public void complete() {
		q.add((final Consumer<? super T> action) -> {
			return false;
		});
	}

	/**
	 * {@inheritDoc}
	 * It may block if the internal queue is full.
	 */
	@Override
	public void exception(final Exception exception) {
		q.add((final Consumer<? super T> action) -> {
			throw new RuntimeException(exception);
		});
	}

	/**
	 * {@inheritDoc}
	 * It may block if the internal queue is full.
	 */
	@Override
	public void next(final T item) {
		q.add((final Consumer<? super T> action) -> {
			action.accept(item);
			return true;
		});
	}
	
	/**
	 * {@inheritDoc}
	 * Note: if the internal queue is empty then this call will block -- it will not return false.
	 * False will only be returned when the internal "complete item" is encountered.
	 */
	@Override
	public boolean tryAdvance(final Consumer<? super T> action) {
		try {
			return q.take().apply(action);
		}
		catch (final InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * @return Null, indicating that no splitting is possible.
	 */
	@Override
	public Spliterator<T> trySplit() {
		return null;
	}
	
	/**
	 * @return Long.MAX_VALUE.
	 */
	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}
	
	/**
	 * @return 0.
	 */
	@Override
	public int characteristics() {
		return 0;
	}
	
}
