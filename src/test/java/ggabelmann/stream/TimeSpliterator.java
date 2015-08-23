package ggabelmann.stream;

import java.util.Random;
import java.util.Spliterator;

/**
 * This class is provided as an example.
 * It should be started in a separate thread and will then produce a stream of times (longs).
 * 
 * @author Greg Gabelmann
 * @see ObservableTest
 */
class TimeSpliterator extends AsyncSpliterator<Long> {
	private final int count = 10;
	
	@Override
	public int characteristics() {
		return Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED;
	}
	
	@Override
	public long estimateSize() {
		return count;
	}
	
	/**
	 * @return Null, indicating that no splitting is possible.
	 */
	@Override
	public Spliterator trySplit() {
		return null;
	}

	@Override
	public void run() {
		for (int i = 0; i < count; i++) {
			enqueue(System.currentTimeMillis());
			try {
				Thread.sleep(new Random().nextInt(500) + 500);
			}
			catch (final InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
		done();
	}
	
}
