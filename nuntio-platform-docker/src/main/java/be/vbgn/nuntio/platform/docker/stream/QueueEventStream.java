package be.vbgn.nuntio.platform.docker.stream;

import be.vbgn.nuntio.api.platform.stream.EventStream;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public class QueueEventStream<T> implements EventStream<T> {

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
