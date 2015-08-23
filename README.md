# stream
Classes and utilities that add functionality to Java's Stream framework.

The classes mostly fall into two groups:

1. Simple implementations of the Observable and Observer interfaces from [RxJava](https://github.com/ReactiveX/RxJava).
2. Implementations of the [Spliterator](http://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html) interface that allow:
  1. async code to create items.
  2. an Observer to capture events and turn them into items.

Java's Stream framework is very useful, but these classes make it even better by implementing missing producer/consumer functionality. 
Depending on the situation, it may also be possible to avoid using (and learning) the RxJava library.

# Usage

The code is packaged as a simple Jar.

# Future

- Use in real-world code and refactor accordingly.
- More tests.