package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceEvent;
import be.vbgn.nuntio.api.platform.PlatformServiceIdentifier;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "nuntio.engine.live.enabled", matchIfMissing = true)
@AllArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class LiveWatchDaemon {

    private ServicePlatform platform;
    private ServiceRegistry registry;
    private PlatformToRegistryMapper platformToRegistryMapper;
    private ChecksProcessor healthcheckProcessor;
    private EngineConfig engineConfig;

    @Scheduled(fixedDelayString = "${nuntio.engine.live.delay:PT1S}")
    void liveWatch() {
        if (engineConfig.getLive().isBlocking()) {
            log.trace("Starting livewatch in blocking mode");
            platform.eventStream().forEachBlocking(this::handleEvent);
        } else {
            log.trace("Starting livewatch in polling mode");
            platform.eventStream().forEach(this::handleEvent);
        }
    }

    private void handleEvent(PlatformServiceEvent platformServiceEvent) {
        switch (platformServiceEvent.getEventType()) {
            case STOP:
                findServiceRegistryIdentifiers(platformServiceEvent.getIdentifier())
                        .forEach(registry::unregisterService);
                break;
            case START:
                platform.find(platformServiceEvent.getIdentifier())
                        .ifPresent(platformServiceDescription -> {
                            platformToRegistryMapper.createServices(platformServiceDescription)
                                    .forEach(registryServiceDescription -> {
                                        RegistryServiceIdentifier registryServiceIdentifier = registry.registerService(
                                                registryServiceDescription);
                                        healthcheckProcessor.updateChecks(registryServiceIdentifier,
                                                platformServiceDescription);
                                    });
                        });
                // no break
            case PAUSE:
            case UNPAUSE:
            case HEALTHCHECK:
                platform.find(platformServiceEvent.getIdentifier())
                        .ifPresent(platformServiceDescription -> {
                            registry.findAll(platformServiceDescription)
                                    .forEach(registryIdentifier -> healthcheckProcessor.updateChecks(
                                            registryIdentifier, platformServiceDescription));
                        });

        }
    }


    public Stream<RegistryServiceIdentifier> findServiceRegistryIdentifiers(PlatformServiceIdentifier identifier) {
        return platform.find(identifier)
                .stream()
                .flatMap(platformServiceDescription -> platformServiceDescription.getServiceConfigurations().stream())
                .flatMap(serviceConfiguration -> registry.find(serviceConfiguration.getSharedIdentifier()).stream());
    }

}
