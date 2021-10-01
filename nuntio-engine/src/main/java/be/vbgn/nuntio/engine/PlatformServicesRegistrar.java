package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PlatformServicesRegistrar {

    private final ServicePlatform platform;
    private final ServiceRegistry registry;
    private PlatformToRegistryMapper platformToRegistryMapper;
    private ChecksProcessor healthcheckProcessor;

    public void registerPlatformServices() {
        for (PlatformServiceDescription platformServiceDescription : platform.findAll()) {
            Set<RegistryServiceIdentifier> registryService = registry.findAll(platformServiceDescription);

            registryService.forEach(registryServiceIdentifier -> {
                if (platformServiceDescription.getState() == PlatformServiceState.STOPPED) {
                    log.info("Found registered {}, but platform {} is stopped", registryServiceIdentifier,
                            platformServiceDescription);
                    registry.unregisterService(registryServiceIdentifier);
                }
            });
            Set<RegistryServiceDescription> registryServiceDescriptions = platformToRegistryMapper.createServices(
                    platformServiceDescription);

            if (registryService.isEmpty() && !registryServiceDescriptions.isEmpty()) {
                log.warn("Platform {} is not registered.", platformServiceDescription);
            }

            registryServiceDescriptions.forEach(registryServiceDescription -> {
                RegistryServiceIdentifier serviceIdentifier = registry.registerService(registryServiceDescription);
                healthcheckProcessor.updateChecks(serviceIdentifier, platformServiceDescription);
                registry.updateCheck(serviceIdentifier, CheckType.HEARTBEAT, CheckStatus.PASSING, "Nuntio is watching");
            });
        }
    }
}
