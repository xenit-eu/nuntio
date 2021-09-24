package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.management.BlockingScheduledTasksManager;
import be.vbgn.nuntio.api.platform.stream.EventStream;
import be.vbgn.nuntio.platform.docker.stream.QueueEventStream;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Lazy
@ConditionalOnBean(DockerClient.class)
public class DockerContainerWatcher {

    private final DockerClient dockerClient;

    private final AtomicBoolean isWatching = new AtomicBoolean(false);

    private BlockingQueue<Event> events;
    private final BlockingScheduledTasksManager blockingScheduledTasksManager;

    @Autowired
    public DockerContainerWatcher(DockerClient dockerClient,
            BlockingScheduledTasksManager blockingScheduledTasksManager) {
        this.dockerClient = dockerClient;
        this.blockingScheduledTasksManager = blockingScheduledTasksManager;
    }

    @Scheduled(fixedRateString = "${nuntio.docker.watch.rate:PT1S}")
    public void startWatching() {
        if (!isWatching.compareAndExchange(false, true)) {
            blockingScheduledTasksManager.registerBlockingTask(this);
            events = new SynchronousQueue<>();
            dockerClient.eventsCmd()
                    .withEventTypeFilter(EventType.CONTAINER)
                    .withEventFilter("start", "die", "pause", "unpause", "health_status")
                    .exec(new EventResultCallback());
        }
    }

    public EventStream<Event> getEventStream() {

        return new QueueEventStream<>(events);
    }

    private class EventResultCallback extends Adapter<Event> {

        @Override
        public void onStart(Closeable closeable) {
            isWatching.set(true);
            super.onStart(closeable);
            log.info("Started watching for docker events");
        }

        @SneakyThrows
        @Override
        public void onNext(Event object) {
            log.debug("Received event {}", object);
            events.put(object);
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("Error during event watching", throwable);
            super.onError(throwable);
        }

        @Override
        public void onComplete() {
            isWatching.set(false);
            log.info("Finished watching for docker event");
            super.onComplete();
        }
    }
}
