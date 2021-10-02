package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceEvent;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.LiveWatchProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class LiveWatchDaemon implements Runnable {

    private ServicePlatform platform;
    private ServiceRegistry registry;
    private ServiceMapper serviceMapper;
    private LiveWatchProperties liveWatchProperties;

    @Override
    public void run() {
        while (true) {
            try {
                if (liveWatchProperties.isBlocking()) {
                    log.debug("Starting livewatch in blocking mode");
                    platform.eventStream().forEachBlocking(this::handleEvent);
                } else {
                    Instant startTime = Instant.now();
                    log.debug("Starting livewatch in polling mode");
                    platform.eventStream().forEach(this::handleEvent);
                    Duration spentTime = Duration.between(startTime, Instant.now());
                    Duration sleepTime = liveWatchProperties.getDelay().minus(spentTime);
                    Thread.sleep(sleepTime.toMillis());
                }
            } catch (Throwable e) {
                log.error("Error during livewatch", e);
            }
        }
    }

    private void handleEvent(PlatformServiceEvent platformServiceEvent) {
        switch (platformServiceEvent.getEventType()) {
            case START:
                platform.find(platformServiceEvent.getIdentifier())
                        .ifPresent(platformServiceDescription -> {
                            serviceMapper.registerService(platformServiceDescription);
                        });
                break;
            case PAUSE:
            case UNPAUSE:
            case HEALTHCHECK:
                platform.find(platformServiceEvent.getIdentifier())
                        .ifPresent(platformServiceDescription -> {
                            serviceMapper.updateServiceChecks(platformServiceDescription);
                        });
                break;
            case STOP:
                registry.findAll(platformServiceEvent.getIdentifier().getSharedIdentifier())
                        .forEach(registry::unregisterService);
                break;

        }
    }
}
