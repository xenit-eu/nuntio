package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceEvent;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.LiveWatchProperties;
import be.vbgn.nuntio.engine.diff.AddService;
import be.vbgn.nuntio.engine.diff.DiffResolver;
import be.vbgn.nuntio.engine.diff.DiffUtil;
import be.vbgn.nuntio.engine.diff.RemoveService;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class LiveWatchDaemon implements Runnable {

    private ServicePlatform platform;
    private ServiceRegistry registry;
    private DiffResolver diffResolver;
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
                        .ifPresentOrElse(platformServiceDescription -> {
                            DiffUtil.diff(Collections.emptySet(), Collections.singleton(platformServiceDescription))
                                    .peek(diff -> {
                                        diff.cast(AddService.class).ifPresent(addService -> {
                                            log.debug("Registering service {} for started platform {}", addService.getServiceConfiguration(), addService.getDescription());
                                        });
                                    })
                                    .forEach(diffResolver);
                        }, () -> log.error("Failed to register platform service {}: does no longer exist", platformServiceEvent.getIdentifier()));
                break;
            case PAUSE:
            case UNPAUSE:
            case HEALTHCHECK:
                platform.find(platformServiceEvent.getIdentifier())
                        .ifPresentOrElse(platformServiceDescription -> {
                            var registeredServices = registry.findAll(platformServiceDescription.getIdentifier().getPlatformIdentifier());
                            log.debug("Updating service information for platform {} on services {}", platformServiceDescription, registeredServices);
                            DiffUtil.diff(registeredServices, Collections.singleton(platformServiceDescription))
                                    .forEach(diffResolver);
                        }, () -> log.error("Failed to register platform service {}: does no longer exist", platformServiceEvent.getIdentifier()));
                break;
            case STOP:
                var registeredServices = registry.findAll(platformServiceEvent.getIdentifier().getPlatformIdentifier());
                DiffUtil.diff(registeredServices, Collections.emptySet())
                        .peek(diff -> {
                            diff.cast(RemoveService.class).ifPresent(removeService -> {
                                log.debug("Unregister service {} for stopped platform {}", removeService.getRegistryServiceIdentifier(), platformServiceEvent.getIdentifier());
                            });
                        })
                        .forEach(diffResolver);
                break;

        }
    }
}
