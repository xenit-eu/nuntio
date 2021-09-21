package be.vbgn.nuntio.core.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class DockerContainerWatcher {

    private final DockerClient dockerClient;
    private final DockerContainerHandler dockerContainerHandler;

    private final AtomicBoolean isWatching = new AtomicBoolean(false);

    @Scheduled(fixedRateString = "PT1S")
    public void startWatching() {
        if (!isWatching.compareAndExchange(false, true)) {
            dockerClient.eventsCmd()
                    .withEventTypeFilter(EventType.CONTAINER)
                    .withEventFilter("start", "die", "pause", "unpause", "health_status")
                    .exec(new EventResultCallback());
        }
    }


    private class EventResultCallback extends Adapter<Event> {

        @Override
        public void onStart(Closeable closeable) {
            isWatching.set(true);
            super.onStart(closeable);
            log.info("Started watching for docker events");
        }

        @Override
        public void onNext(Event object) {
            log.debug("Received event {}", object);
            dockerContainerHandler.handleEvent(object);
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
