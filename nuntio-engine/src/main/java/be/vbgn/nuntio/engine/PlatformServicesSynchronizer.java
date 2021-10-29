package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.diff.AddService;
import be.vbgn.nuntio.engine.diff.DiffResolver;
import be.vbgn.nuntio.engine.diff.DiffUtil;
import be.vbgn.nuntio.engine.diff.InitialRegistrationResolver;
import be.vbgn.nuntio.engine.diff.RemoveService;
import be.vbgn.nuntio.engine.metrics.OperationMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PlatformServicesSynchronizer {

    private final ServicePlatform platform;
    private final ServiceRegistry registry;
    private final DiffResolver diffResolver;
    private final InitialRegistrationResolver initialRegistrationResolver;
    private final OperationMetrics syncMetrics;

    public void syncServices() {
        DiffUtil.diff(registry.findServices(), platform.findAll())
                .peek(syncMetrics)
                .peek(diff -> {
                    diff.cast(AddService.class).ifPresent(addService -> {
                        log.info("Registering platform {} service {}", addService.getDescription(), addService.getServiceConfiguration());
                    });

                    diff.cast(RemoveService.class).ifPresent(removeService -> {
                        log.info("Removing service {}", removeService.getRegistryServiceIdentifier());
                    });
                })
                .peek(initialRegistrationResolver)
                .forEach(diffResolver);
    }

}
