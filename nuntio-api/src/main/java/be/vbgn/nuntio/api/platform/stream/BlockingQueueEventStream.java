package be.vbgn.nuntio.api.platform.stream;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

@AllArgsConstructor
public class BlockingQueueEventStream<T> implements EventStream<T> {

    @NonNull
    private BlockingQueue<T> queue;

    @Override
    public Optional<? extends T> next() {
        return Optional.ofNullable(queue.poll());
    }

    @Override
    @SneakyThrows
    public T nextBlocking() {
        return queue.take();
    }

}
