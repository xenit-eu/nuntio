package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.identifier.ServiceIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ServiceMapper {
    private ServiceRegistry serviceRegistry;
    private EngineProperties engineProperties;

    private static final String NUNTIO_RESERVED_PREFIX = "nuntio-";

    private Set<RegistryServiceDescription> createServices(PlatformServiceDescription serviceDescription) {
        return serviceDescription.getServiceConfigurations()
                .stream()
                .flatMap(configuration -> configuration.getServiceNames().stream()
                        .peek(serviceName -> log.debug("Creating registry service {} for platform {}", serviceName, configuration))
                        .map(serviceName -> RegistryServiceDescription.builder()
                                .name(serviceName)
                                .platformIdentifier(serviceDescription.getIdentifier().getPlatformIdentifier())
                                .serviceIdentifier(ServiceIdentifier.of(serviceDescription.getIdentifier()
                                        .getPlatformIdentifier(), configuration.getServiceBinding()))
                                .address(configuration.getServiceBinding().getIp())
                                .port(configuration.getServiceBinding().getPort().orElseThrow())
                                .tags(configuration.getServiceTags())
                                .metadata(createMetadata(configuration))
                                .build()
                        )
                )
                .collect(Collectors.toSet());
    }

    private Set<RegistryServiceIdentifier> findServicesForPlatform(PlatformServiceDescription platformServiceDescription) {
        return serviceRegistry.findAll(platformServiceDescription.getIdentifier().getPlatformIdentifier());
    }

    private Map<String, String> createMetadata(PlatformServiceConfiguration configuration) {
        final var serviceMetadata = configuration.getServiceMetadata();
        final var internalMetadata = configuration.getInternalMetadata();
        Map<String, String> allMetadata = new HashMap<>(serviceMetadata.size() + internalMetadata.size());
        serviceMetadata.forEach((key, value) -> {
            if (!key.startsWith(NUNTIO_RESERVED_PREFIX)) {
                allMetadata.put(key, value);
            } else {
                log.warn(
                        "Platform configuration {} specified invalid metadata. Metadata key {} is reserved.",
                        configuration, key);
            }
        });
        internalMetadata.forEach((key, value) -> {
            allMetadata.put(NUNTIO_RESERVED_PREFIX + key, value);
        });
        return allMetadata;
    }

    public void registerService(PlatformServiceDescription platformServiceDescription) {
        var services = createServices(platformServiceDescription);
        services.forEach(registryServiceDescription -> {
            RegistryServiceIdentifier registryServiceIdentifier = serviceRegistry.registerService(registryServiceDescription);

            if(engineProperties.getChecks().isHeartbeat()) {
                serviceRegistry.registerCheck(registryServiceIdentifier, CheckType.HEARTBEAT);
                serviceRegistry.updateCheck(registryServiceIdentifier, CheckType.HEARTBEAT, CheckStatus.PASSING, "Nuntio has registered");
            }
            if(engineProperties.getChecks().isHealthcheck() && platformServiceDescription.getHealth().isPresent()) {
                serviceRegistry.registerCheck(registryServiceIdentifier, CheckType.HEALTHCHECK);
            }

            updateHealthCheck(registryServiceIdentifier, platformServiceDescription);

            if(platformServiceDescription.getState() == PlatformServiceState.PAUSED) {
                serviceRegistry.registerCheck(registryServiceIdentifier, CheckType.PAUSE);
                serviceRegistry.updateCheck(registryServiceIdentifier, CheckType.PAUSE, CheckStatus.FAILING,
                        "Service is paused");
            }
        });

    }

    public void unregisterService(PlatformServiceDescription platformServiceDescription) {
        var serviceIdentifiers = findServicesForPlatform(platformServiceDescription);

        serviceIdentifiers.forEach(registryServiceIdentifier -> {
            serviceRegistry.unregisterService(registryServiceIdentifier);
        });
    }

    private void updateHealthCheck(RegistryServiceIdentifier registryServiceIdentifier, PlatformServiceDescription platformServiceDescription) {
        if(engineProperties.getChecks().isHealthcheck()) {
            platformServiceDescription.getHealth().ifPresent(health -> {
                switch(health.getHealthStatus()) {
                    case HEALTHY:
                        serviceRegistry.updateCheck(registryServiceIdentifier, CheckType.HEALTHCHECK, CheckStatus.PASSING,
                                health.getLog());
                        break;
                    case UNHEALTHY:
                        serviceRegistry.updateCheck(registryServiceIdentifier, CheckType.HEALTHCHECK, CheckStatus.FAILING,
                                health.getLog());
                        break;
                    case STARTING:
                        serviceRegistry.updateCheck(registryServiceIdentifier, CheckType.HEALTHCHECK, CheckStatus.WARNING, health.getLog());
                }
            });
        }
    }

    public void updateServiceChecks(PlatformServiceDescription platformServiceDescription) {
        var serviceIdentifiers = findServicesForPlatform(platformServiceDescription);

        serviceIdentifiers.forEach(registryServiceIdentifier -> {
            if(engineProperties.getChecks().isHeartbeat()) {
                serviceRegistry.updateCheck(registryServiceIdentifier, CheckType.HEARTBEAT, CheckStatus.PASSING, "Nuntio is watching");
            }

            updateHealthCheck(registryServiceIdentifier, platformServiceDescription);

            if(platformServiceDescription.getState() == PlatformServiceState.PAUSED) {
                serviceRegistry.registerCheck(registryServiceIdentifier, CheckType.PAUSE);
                serviceRegistry.updateCheck(registryServiceIdentifier, CheckType.PAUSE, CheckStatus.FAILING, "Service is paused");
            } else {
                serviceRegistry.unregisterCheck(registryServiceIdentifier, CheckType.PAUSE);
            }
        });
    }

}
