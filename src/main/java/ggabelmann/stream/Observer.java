package ggabelmann.stream;

/**
 * This class is a simplified version of {@link http://reactivex.io/RxJava/javadoc/rx/Observer.html}.
 * 
 * @author Greg Gabelmann
 */
public interface Observer<T> {
	
	/**
	 * A signal to this object that no more elements are coming.
	 */
	public void complete();
	
	/**
	 * Called if the Observable experienced an exception of some kind.
	 * 
	 * @param exception The Exception.
	 * @throws IllegalArgumentException If the given Exception is null.
	 */
	public void exception(final Exception exception);
	
	/**
	 * The next item to process.
	 * 
	 * @param item The item.
	 * @throws IllegalArgumentException if the given item is null.
	 */
	public void next(final T item);
	
}
