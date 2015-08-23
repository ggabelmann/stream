package ggabelmann.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

/**
 * This class allows a group of Callable objects to be used as the source of items in a Stream.
 * Each Callable is run, perhaps in parallel, and the results are the items in the Stream.
 * 
 * @author Greg Gabelmann
 * @see ObservableTest
 */
public class ExecutorSpliterator<R> extends AsyncSpliterator<R> {
	
	private final CompletionService<R> cs;
	private final List<Callable<R>> tasks;
	
	/**
	 * Constructor.
	 * Uses the thread that calls run() to serially execute the given callables.
	 * This is useful for testing but not recommended for other uses.
	 * 
	 * The given Iterable must be finite or else the run() will not work correctly.
	 * 
	 * @param callables The Callable objects.
	 * @throws IllegalArgumentException If the given Iterable is null.
	 */
	public ExecutorSpliterator(final Iterable<Callable<R>> callables) {
		this(callables, (final Runnable r) -> r.run());
	}
	
	/**
	 * Constructor.
	 * The given Iterable must be finite or else the run() will not work correctly.
	 * 
	 * @param callables The Callable objects.
	 * @param e The Executor used for executing the given callables.
	 * @throws IllegalArgumentException If the given Executor is null.
	 * @throws IllegalArgumentException If the given Iterable is null.
	 */
	public ExecutorSpliterator(final Iterable<Callable<R>> callables, final Executor e) {
		if (callables == null) throw new IllegalArgumentException();
		if (e == null) throw new IllegalArgumentException();
		
		this.cs = new ExecutorCompletionService<>(e);
		this.tasks = new ArrayList<>();
		callables.forEach(c -> tasks.add(c));
	}
	
	/**
	 * @return 0.
	 */
	@Override
	public int characteristics() {
		return 0;
	}
	
	/**
	 * @return The number of Callable objects.
	 */
	@Override
	public long estimateSize() {
		return tasks.size();
	}
	
	/**
	 * @return Null, indicating that no splitting is possible.
	 */
	@Override
	public Spliterator trySplit() {
		return null;
	}
	
	/**
	 * First starts all the Callable objects using the given Executor object.
	 * Then enqueues the results as they finish (so that tryAdvance() can consume them), in whatever order that may be.
	 * Note: the behavior of the execution depends highly on the Executor used.
	 * 
	 * Once all Callable objects finish the "done flag" will be set.
	 */
	@Override
	public void run() {
		for (final Callable<R> c : tasks) {
			cs.submit(c);
		}
		for (int i = 0; i < tasks.size(); i++) {
			try {
				enqueue(cs.take().get());
			}
			catch (final ExecutionException | InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
		done();
	}
	
	public static class Functions {
		
		/**
		 * Adapts a Future to the Callable interface.
		 * There is no way to "start" a future, so there must be a thread computing its result already.
		 * The thread that calls call() will execute get().
		 * 
		 * @param <T> The type.
		 * @param future The future to adapt.
		 * @return The Callable.
		 */
		public static <T> Callable<T> futureAsCallable(final Future<T> future) {
			return () -> {
				return future.get();
			};
		}
		
		/**
		 * Adapts a RunnableFuture to the Callable interface.
		 * The thread that calls call() will execute run() and then get().
		 * 
		 * @param <T> The type.
		 * @param future The runnable future to adapt.
		 * @return The Callable.
		 */
		public static <T> Callable<T> runnableFutureAsCallable(final RunnableFuture<T> future) {
			return () -> {
				future.run();
				return future.get();
			};
		}
		
		/**
		 * Adapts a Runnable to the Callable interface.
		 * The thread that calls call() will execute run() and then return the given result.
		 * 
		 * @param <T> The type.
		 * @param future The runnable to adapt.
		 * @param result The result to return from the call to call().
		 * @return The Callable.
		 */
		public static <T> Callable<T> runnableAsCallable(final Runnable runnable, final T result) {
			return () -> {
				runnable.run();
				return result;
			};
		}
		
	}
	
}
