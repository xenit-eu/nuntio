package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class AntiEntropyDaemon {

    private ServicePlatform platform;
    private ServiceRegistry registry;
    private ChecksProcessor healthcheckProcessor;
    private PlatformServicesRegistrar platformServicesRegistrar;

    public void runAntiEntropy() {
        try {
            log.debug("Running anti-entropy");

            // Remove services that have been published in any registry but have been removed from docker
            // Also updates healthchecks for services that still exist
            for (RegistryServiceIdentifier service : registry.findServices()) {
                var platformService = platform.find(service.getSharedIdentifier());
                platformService.ifPresentOrElse(platformServiceDescription -> {
                    healthcheckProcessor.updateChecks(service, platformServiceDescription);
                }, () -> {
                    log.info("Found registered {}, but platform is no longer present", service);
                    registry.unregisterService(service);
                });
            }

            platformServicesRegistrar.registerAllServices();
        } catch (Throwable e) {
            log.error("Exception during anti-entropy run", e);
        }
    }

}
