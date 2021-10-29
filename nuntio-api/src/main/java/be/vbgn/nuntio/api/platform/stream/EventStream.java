package be.vbgn.nuntio.api.platform.stream;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface EventStream<T> {

    Optional<? extends T> next();

    T nextBlocking();

    default void forEach(Consumer<? super T> consumer) {
        Optional<? extends T> next = next();
        while (next.isPresent()) {
            consumer.accept(next.get());
            next = next();
        }
    }

    default void forEachBlocking(Consumer<? super T> consumer) {
        while (true) {
            T next = nextBlocking();
            consumer.accept(next);
        }
    }

    default EventStream<T> peek(Consumer<? super T> peeker) {
        return map(item -> {
            peeker.accept(item);
            return item;
        });
    }

    default <U> EventStream<U> map(Function<? super T, ? extends U> mapper) {
        return new MappedEventStream<>(this, mapper);
    }

    default <U> EventStream<U> flatMap(Function<? super T, ? extends Stream<U>> flatMapper) {
        return new FlatMappedEventStream<>(this, flatMapper);
    }
}
