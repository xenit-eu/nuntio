package eu.xenit.nuntio.api.platform.stream;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public class FakeEventStream<T> implements EventStream<T> {

    private final Queue<T> queue = new LinkedTransferQueue<>();

    public void appendItem(T item) {
        queue.add(item);
    }

    @Override
    public Optional<? extends T> next() {
        return Optional.ofNullable(queue.poll());
    }

    @Override
    public T nextBlocking() {
        return queue.remove();
    }
}
