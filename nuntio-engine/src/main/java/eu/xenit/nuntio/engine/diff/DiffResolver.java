package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.PlatformServiceState;
import eu.xenit.nuntio.api.registry.CheckStatus;
import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.api.registry.errors.ServiceDeregistrationException;
import eu.xenit.nuntio.api.registry.errors.ServiceOperationException;
import eu.xenit.nuntio.api.registry.errors.ServiceRegistrationException;
import eu.xenit.nuntio.engine.EngineProperties;
import eu.xenit.nuntio.engine.failure.FailureReporter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DiffResolver implements Consumer<Diff> {

    private ServiceRegistry registry;
    private FailureReporter failureReporter;
    private EngineProperties engineProperties;

    private static final String NUNTIO_RESERVED_PREFIX = "nuntio-";

    @Override
    public void accept(Diff diff) {
        diff.cast(AddService.class).ifPresent(addService -> {
            registerService(addService.getDescription(), addService.getServiceConfiguration());
        });

        diff.cast(EqualService.class).ifPresent(equalService -> {
            try {
                updateServiceChecks(equalService.getDescription(), equalService.getServiceConfiguration(),
                        equalService.getRegistryServiceIdentifier());
            } catch(ServiceOperationException e) {
                log.error("Failed to update service {} checks", equalService.getRegistryServiceIdentifier(), e);
                failureReporter.reportRegistryFailure(e);
            }
        });

        diff.cast(RemoveService.class).ifPresent(removeService -> {
            try {
                registry.unregisterService(removeService.getRegistryServiceIdentifier());
            } catch(ServiceDeregistrationException e) {
                log.error("Failed to remove service {}", removeService.getRegistryServiceIdentifier(), e);
                failureReporter.reportRegistryFailure(e);
            }
        });
    }

    private void registerService(PlatformServiceDescription platformServiceDescription, PlatformServiceConfiguration platformServiceConfiguration) {
        platformServiceConfiguration.getServiceNames().stream()
                .peek(serviceName -> log.debug("Creating registry service {} for platform {}", serviceName,
                        platformServiceConfiguration))
                .map(serviceName -> RegistryServiceDescription.builder()
                        .name(serviceName)
                        .platformIdentifier(platformServiceDescription.getIdentifier().getPlatformIdentifier())
                        .serviceIdentifier(ServiceIdentifier.of(platformServiceDescription.getIdentifier()
                                .getPlatformIdentifier(), platformServiceConfiguration.getServiceBinding()))
                        .address(platformServiceConfiguration.getServiceBinding().getIp())
                        .port(platformServiceConfiguration.getServiceBinding().getPort().orElseThrow())
                        .tags(platformServiceConfiguration.getServiceTags())
                        .checks(platformServiceConfiguration.getChecks())
                        .metadata(createMetadata(platformServiceConfiguration))
                        .build()
                )
                .forEach(service -> {
                    try {
                        RegistryServiceIdentifier registryServiceIdentifier = registry.registerService(service);

                        if(engineProperties.getChecks().isHeartbeat()) {
                            registry.registerCheck(registryServiceIdentifier, CheckType.HEARTBEAT);
                            registry.updateCheck(registryServiceIdentifier, CheckType.HEARTBEAT, CheckStatus.PASSING,
                                    "Nuntio has registered\n"
                                            + platformServiceDescription.getIdentifier() + "\n"
                                            + platformServiceConfiguration.getServiceBinding()
                            );
                        }
                        if (engineProperties.getChecks().isHealthcheck() && platformServiceDescription.getHealth()
                                .isPresent()) {
                            registry.registerCheck(registryServiceIdentifier, CheckType.HEALTHCHECK);
                        }

                        updateHealthCheck(registryServiceIdentifier, platformServiceDescription);

                        if(platformServiceDescription.getState() == PlatformServiceState.PAUSED) {
                            registry.registerCheck(registryServiceIdentifier, CheckType.PAUSE);
                            registry.updateCheck(registryServiceIdentifier, CheckType.PAUSE, CheckStatus.FAILING,
                                    "Service is paused");
                        }
                    } catch (ServiceRegistrationException e) {
                        log.error("Failed to register service {}", service, e);
                        failureReporter.reportRegistryFailure(e);
                    } catch(ServiceOperationException e) {
                        log.error("Failed to register service {} check", service, e);
                        failureReporter.reportRegistryFailure(e);
                    }
                });
    }

    public void updateServiceChecks(PlatformServiceDescription platformServiceDescription, PlatformServiceConfiguration serviceConfiguration, RegistryServiceIdentifier registryServiceIdentifier) throws ServiceOperationException{
        if(engineProperties.getChecks().isHeartbeat()) {
            registry.updateCheck(registryServiceIdentifier, CheckType.HEARTBEAT, CheckStatus.PASSING,
                    "Nuntio is watching\n"
                            + platformServiceDescription.getIdentifier()+"\n"
                            + serviceConfiguration.getServiceBinding()
            );
        }

        updateHealthCheck(registryServiceIdentifier, platformServiceDescription);

        if(platformServiceDescription.getState() == PlatformServiceState.PAUSED) {
            registry.registerCheck(registryServiceIdentifier, CheckType.PAUSE);
            registry.updateCheck(registryServiceIdentifier, CheckType.PAUSE, CheckStatus.FAILING, "Service is paused");
        } else {
            registry.unregisterCheck(registryServiceIdentifier, CheckType.PAUSE);
        }
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


    private void updateHealthCheck(RegistryServiceIdentifier registryServiceIdentifier, PlatformServiceDescription platformServiceDescription) throws ServiceOperationException{
        if(engineProperties.getChecks().isHealthcheck() && platformServiceDescription.getHealth().isPresent()) {
            var health = platformServiceDescription.getHealth().get();
            switch(health.getHealthStatus()) {
                case HEALTHY:
                    registry.updateCheck(registryServiceIdentifier, CheckType.HEALTHCHECK, CheckStatus.PASSING,
                            health.getLog());
                    break;
                case UNHEALTHY:
                    registry.updateCheck(registryServiceIdentifier, CheckType.HEALTHCHECK, CheckStatus.FAILING,
                            health.getLog());
                    break;
                case STARTING:
                    registry.updateCheck(registryServiceIdentifier, CheckType.HEALTHCHECK, CheckStatus.WARNING, health.getLog());
            }
        }
    }


}
