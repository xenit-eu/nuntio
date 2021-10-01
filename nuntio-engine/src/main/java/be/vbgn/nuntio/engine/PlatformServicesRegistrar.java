package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.platform.ServicePlatform;
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

    public void registerAllServices() {
        for (PlatformServiceDescription platformServiceDescription : platform.findAll()) {
            if (platformServiceDescription.getState() == PlatformServiceState.STOPPED) {
                unregisterService(platformServiceDescription);
            } else {
                registerService(platformServiceDescription);
            }
        }
    }

    public void registerService(PlatformServiceDescription platformServiceDescription) {
        var registryServiceDescriptions = platformToRegistryMapper.createServices(platformServiceDescription);

        registryServiceDescriptions.forEach(registryServiceDescription -> {
            RegistryServiceIdentifier serviceIdentifier = registry.registerService(registryServiceDescription);
            healthcheckProcessor.updateChecks(serviceIdentifier, platformServiceDescription);
        });
    }

    public void unregisterService(PlatformServiceDescription platformServiceDescription) {
        Set<RegistryServiceIdentifier> registryServices = registry.findAll(platformServiceDescription);
        registryServices.forEach(registry::unregisterService);
    }
}
