package be.vbgn.nuntio.api.platform.stream;

import java.util.Optional;
import java.util.function.Function;

class MappedEventStream<T, V> implements EventStream<V> {

    private final EventStream<? extends T> originalStream;
    private final Function<? super T, ? extends V> mapper;

    MappedEventStream(EventStream<? extends T> originalStream, Function<? super T, ? extends V> mapper) {
        this.originalStream = originalStream;
        this.mapper = mapper;
    }

    @Override
    public Optional<? extends V> next() {
        return originalStream.next().map(mapper);
    }

    @Override
    public V nextBlocking() {
        return mapper.apply(originalStream.nextBlocking());
    }

    @Override
    public <U> EventStream<U> map(Function<? super V, ? extends U> mapper) {
        return new MappedEventStream<T, U>(originalStream, this.mapper.andThen(mapper));
    }
}
