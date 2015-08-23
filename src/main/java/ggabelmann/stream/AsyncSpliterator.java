package ggabelmann.stream;

import java.util.Spliterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * This class allows an asynchronous Thread to act as a source of items in a Stream.
 * It does this by implementing the Runnable and Spliterator interfaces.
 * An internal queue is used to separate the production and consumption of items.
 * 
 * This class is useful because it allows production and consumption of items to occur in parallel.
 * A normal single-threaded Spliterator can make calls that block or take a lot of CPU time, but that will
 * halt all other work (eg, the terminal operation).
 * 
 * The Stream framework guarantees that Spliterators will be used by a single thread at a time.
 * Therefor, this class is not thread-safe unless otherwise noted by a subclass.
 * Note: a lock is used internally so that the internal queue is not corrupted when it's being accessed by both
 * the internal and external threads. That does not mean tryAdvance() (for example) can be called by two external
 * Threads unless a subclass says that explicitly.
 * 
 * The stream of items can be bounded or unbounded.
 * The decision is up to the subclass.
 * However, once the stream is "done" it can't be reused unless the subclass allows it.
 * The author does not recommend allowing it.
 * 
 * @author Greg Gabelmann
 */
public abstract class AsyncSpliterator<T> implements Runnable, Spliterator<T> {
	
	private final Condition changed;
	private boolean done;
	private final Lock lock;
	private final Condition notFull;
	private final ArrayBlockingQueue<T> q;
	
	/**
	 * Constructor which uses a default queue size of 1.
	 */
	public AsyncSpliterator() {
		this(1);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param queueSize The size.
	 * @throws IllegalArgumentException if the given queueSize is less than 1.
	 */
	public AsyncSpliterator(final int queueSize) {
		if (queueSize < 1) throw new IllegalArgumentException();
		
		this.done = false;
		this.lock = new ReentrantLock();
		this.changed = lock.newCondition();
		this.notFull = lock.newCondition();
		this.q = new ArrayBlockingQueue(queueSize);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Calls will block if there are no items in the internal queue.
	 */
	@Override
	public boolean tryAdvance(final Consumer<? super T> action) {
		T s = null;
		
		lock.lock();
		try {
			// The loop is necessary because the await() call may return randomly.
			for (;;) {
				if (q.size() > 0) {
					s = q.take();
					notFull.signal();
					break;
				}
				else if (done) {
					return false;
				}
				else {
					changed.await();
				}
			}
		}
		catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
		
		action.accept(s);
		return true;
	}

	/**
	 * Adds the given item to the internal queue.
	 * It will block until the queue is not full.
	 * 
	 * Note to self: the locking really is necessary.
	 * We have to protect against enqueue() and done() being interleaved with the check in tryAdvance()
	 * for size() > 0 and done == true.
	 */
	protected void enqueue(final T t) {
		lock.lock();
		try {
			// The loop is necessary because the await() may return randomly.
			for (;;) {
				if (q.offer(t)) {
					changed.signal();
					break;
				}
				else {
					notFull.await();
				}
			}
		}
		catch (final InterruptedException ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
	}
	
	/**
	 * See above for note on locking.
	 */
	protected void done() {
		lock.lock();
		try {
			done = true;
			changed.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
}
