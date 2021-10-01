package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.CheckProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ChecksProcessor {

    private ServiceRegistry registry;
    private CheckProperties healthCheckProperties;

    public void updateChecks(RegistryServiceIdentifier service,
            PlatformServiceDescription platformServiceDescription) {
        switch (platformServiceDescription.getState()) {
            case RUNNING:
                registry.unregisterCheck(service, CheckType.PAUSE);
                break;
            case PAUSED:
                registry.updateCheck(service, CheckType.PAUSE, CheckStatus.FAILING, "Container is paused");
                break;
            case STOPPED:
                log.info("Found registered {}, but platform {} is stopped", service,
                        platformServiceDescription);
                registry.unregisterService(service);
                // Stopped services do not update their platform health anymore
                // because they are unregistered
                return;
        }

        if (platformServiceDescription.getState() != PlatformServiceState.STOPPED
                && healthCheckProperties.isHeartbeat()) {
            registry.updateCheck(service, CheckType.HEARTBEAT, CheckStatus.PASSING, "Nuntio is watching");
        }

        if (healthCheckProperties.isHealthcheck()) {
            platformServiceDescription.getHealth().ifPresentOrElse(platformServiceHealth -> {
                switch (platformServiceHealth.getHealthStatus()) {
                    case HEALTHY:
                        registry.updateCheck(service, CheckType.HEALTHCHECK, CheckStatus.PASSING,
                                platformServiceHealth.getLog());
                        break;
                    case STARTING:
                        registry.updateCheck(service, CheckType.HEALTHCHECK, CheckStatus.WARNING,
                                platformServiceHealth.getLog());
                        break;
                    case UNHEALTHY:
                        registry.updateCheck(service, CheckType.HEALTHCHECK, CheckStatus.FAILING,
                                platformServiceHealth.getLog());
                        break;
                }

            }, () -> {
                registry.unregisterCheck(service, CheckType.HEALTHCHECK);
            });
        }
    }


}
