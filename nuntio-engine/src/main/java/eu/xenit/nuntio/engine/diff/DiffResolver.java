package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.PlatformServiceState;
import eu.xenit.nuntio.api.registry.CheckStatus;
import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.EngineProperties;
import eu.xenit.nuntio.engine.mapper.PlatformToRegistryMapper;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DiffResolver implements Consumer<Diff> {

    private ServiceRegistry registry;
    private EngineProperties engineProperties;
    private PlatformToRegistryMapper platformToRegistryMapper;

    @Override
    public void accept(Diff diff) {
        diff.cast(AddService.class).ifPresent(addService -> {
            registerService(addService.getDescription(), addService.getServiceConfiguration());
        });

        diff.cast(EqualService.class).ifPresent(equalService -> {
            updateServiceChecks(equalService.getDescription(), equalService.getServiceConfiguration(), equalService.getRegistryServiceIdentifier());
        });

        diff.cast(RemoveService.class).ifPresent(removeService -> {
            registry.unregisterService(removeService.getRegistryServiceIdentifier());
        });
    }

    private void registerService(PlatformServiceDescription platformServiceDescription, PlatformServiceConfiguration platformServiceConfiguration) {
        platformToRegistryMapper.registryServiceDescriptionsFor(platformServiceDescription, platformServiceConfiguration)
                .forEach(service -> {
                    RegistryServiceIdentifier registryServiceIdentifier = registry.registerService(service);

                    if(engineProperties.getChecks().isHeartbeat()) {
                        registry.registerCheck(registryServiceIdentifier, CheckType.HEARTBEAT);
                        registry.updateCheck(registryServiceIdentifier, CheckType.HEARTBEAT, CheckStatus.PASSING,
                                "Nuntio has registered\n"
                                        + platformServiceDescription.getIdentifier()+"\n"
                                        + platformServiceConfiguration.getServiceBinding()
                        );
                    }
                    if(engineProperties.getChecks().isHealthcheck() && platformServiceDescription.getHealth().isPresent()) {
                        registry.registerCheck(registryServiceIdentifier, CheckType.HEALTHCHECK);
                    }

                    updateHealthCheck(registryServiceIdentifier, platformServiceDescription);

                    if(platformServiceDescription.getState() == PlatformServiceState.PAUSED) {
                        registry.registerCheck(registryServiceIdentifier, CheckType.PAUSE);
                        registry.updateCheck(registryServiceIdentifier, CheckType.PAUSE, CheckStatus.FAILING,
                                "Service is paused");
                    }
                });
    }

    public void updateServiceChecks(PlatformServiceDescription platformServiceDescription, PlatformServiceConfiguration serviceConfiguration, RegistryServiceIdentifier registryServiceIdentifier) {
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


    private void updateHealthCheck(RegistryServiceIdentifier registryServiceIdentifier, PlatformServiceDescription platformServiceDescription) {
        if(engineProperties.getChecks().isHealthcheck()) {
            platformServiceDescription.getHealth().ifPresent(health -> {
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
            });
        }
    }


}
