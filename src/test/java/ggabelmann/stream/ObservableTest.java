package ggabelmann.stream;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests and examples
 * 
 * @author Greg Gabelmann
 */
public class ObservableTest {
	
	public ObservableTest() {
	}
	
	@Test
	public void simple() {
		final Observer o = mock(Observer.class);
		final Observable<String> ob = Observable.<String>newBuilder()
			.from(Stream.of("one"))
			.subscribe(o)
			.build();
		
		ob.emit();
		verify(o).complete();
		verify(o, never()).exception(any());
		verify(o).next("one");
	}
	
	@Test
	public void observerSpliterator() {
		final ObserverSpliterator<String> o = new ObserverSpliterator<>();
		o.next("zero");
		o.complete();
		
		o.forEachRemaining(s -> {
			assertTrue(s.equals("zero"));
		});
	}
	
	@Test
	public void time() {
		final AsyncSpliterator<Long> it = new TimeSpliterator();
		
		final Stream<String> stream = StreamSupport.<Long>stream(it, false)
				.map(String::valueOf)
				.peek((final String s) -> {
					try {
						Thread.sleep(new Random().nextInt(500) + 500);
					}
					catch (final InterruptedException ex) {
						throw new RuntimeException(ex);
					}
				});
		
		final Observable<String> observable = Observable.<String>newBuilder()
			.from(stream)
			.subscribe(new StringObserver())
			.build();
		
		new Thread(it).start();
		observable.emit();
		// todo: assert something.
	}
	
	@Test
	public void executor() {
		final AsyncSpliterator<String> it = new ExecutorSpliterator<>(Arrays.asList(
			new StringCallable(0),
			new StringCallable(1),
			new StringCallable(2)));
		
		final Stream<String> stream = StreamSupport.<String>stream(it, false);
		
		final Observable<String> observable = Observable.<String>newBuilder()
			.from(stream)
			.subscribe(new StringObserver())
			.build();
		
		new Thread(it).start();
		observable.emit();
		// todo: assert something.
	}
	
	private static class StringCallable implements Callable<String> {
		private final int id;
		private StringCallable(final int id) {
			this.id = id;
		}
		
		@Override
		public String call() throws Exception {
			try {
				Thread.sleep(new Random().nextInt(500) + 500);
			}
			catch (final InterruptedException ex) {
				throw new RuntimeException(ex);
			}
			return String.valueOf(id);
		}
		
	}
	
	private static class StringObserver implements Observer<String> {
		
		@Override
		public void complete() {
			System.out.println("complete");
		}
		
		@Override
		public void exception(final Exception exception) {
			System.out.println(exception);
		}
		
		@Override
		public void next(String time) {
			System.out.println(time);
		}
	}
	
}
