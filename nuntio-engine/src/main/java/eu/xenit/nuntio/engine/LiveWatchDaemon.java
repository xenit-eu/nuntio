package eu.xenit.nuntio.engine;

import eu.xenit.nuntio.api.platform.PlatformServiceEvent;
import eu.xenit.nuntio.api.platform.ServicePlatform;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.EngineProperties.LiveWatchProperties;
import eu.xenit.nuntio.engine.availability.AvailabilityManager;
import eu.xenit.nuntio.engine.diff.AddService;
import eu.xenit.nuntio.engine.diff.DiffResolver;
import eu.xenit.nuntio.engine.diff.DiffService;
import eu.xenit.nuntio.engine.diff.EqualService;
import eu.xenit.nuntio.engine.diff.RemoveService;
import eu.xenit.nuntio.engine.metrics.LiveWatchMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class LiveWatchDaemon implements Runnable {

    private ServicePlatform platform;
    private ServiceRegistry registry;
    private DiffService diffService;
    private DiffResolver diffResolver;
    private LiveWatchMetrics liveWatchMetrics;
    private LiveWatchProperties liveWatchProperties;
    private AvailabilityManager availabilityManager;

    @Override
    public void run() {
        while (true) {
            if(!liveWatchProperties.isEnabled()) {
                return;
            }
            try {
                if (liveWatchProperties.isBlocking()) {
                    log.info("Starting livewatch in blocking mode");
                    liveWatchMetrics.blockingTime(() -> platform.eventStream().peek(_event -> {
                        availabilityManager.registerSuccess(this);
                    }).forEachBlocking(this::handleEvent));
                } else {
                    log.info("Starting livewatch in polling mode");
                    while(true) {
                        Instant startTime = Instant.now();
                        AtomicInteger processedEvents = new AtomicInteger();
                        liveWatchMetrics.pollingTime(() -> {
                            platform.eventStream()
                                    .peek(_unused -> processedEvents.incrementAndGet())
                                    .forEach(this::handleEvent);
                        });
                        Duration spentTime = Duration.between(startTime, Instant.now());
                        Duration sleepTime = liveWatchProperties.getDelay().minus(spentTime);
                        log.debug("Processed {} events in {}.", processedEvents.get(), spentTime);
                        availabilityManager.registerSuccess(this);
                        log.trace("Sleeping for {}", sleepTime);
                        Thread.sleep(sleepTime.toMillis());
                        log.trace("Wakeup from sleep.");
                    }
                }
                availabilityManager.registerSuccess(this);
            } catch(InterruptedException e) {
                log.info("Caught interrupt. Shutting down live-watch daemon");
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                log.error("Error during livewatch", e);
                liveWatchMetrics.failure();
                availabilityManager.registerFailure(this);
            }
        }
    }

    private void handleEvent(PlatformServiceEvent platformServiceEvent) {
        switch (platformServiceEvent.getEventType()) {
            case START:
                platform.find(platformServiceEvent.getIdentifier())
                        .ifPresentOrElse(platformServiceDescription -> {
                            diffService.diff(Collections.emptySet(), Collections.singleton(platformServiceDescription))
                                    .filter(diff -> diff.cast(AddService.class).isPresent())
                                    .peek(liveWatchMetrics)
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
                            diffService.diff(registeredServices, Collections.singleton(platformServiceDescription))
                                    .filter(diff -> diff.cast(EqualService.class).isPresent())
                                    .peek(liveWatchMetrics)
                                    .forEach(diffResolver);
                        }, () -> log.error("Failed to register platform service {}: does no longer exist", platformServiceEvent.getIdentifier()));
                break;
            case STOP:
                var registeredServices = registry.findAll(platformServiceEvent.getIdentifier().getPlatformIdentifier());
                diffService.diff(registeredServices, Collections.emptySet())
                        .filter(diff -> diff.cast(RemoveService.class).isPresent())
                        .peek(liveWatchMetrics)
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
