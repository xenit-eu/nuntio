package eu.xenit.nuntio.engine;

import eu.xenit.nuntio.api.platform.ServicePlatform;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.diff.AddService;
import eu.xenit.nuntio.engine.diff.DiffResolver;
import eu.xenit.nuntio.engine.diff.DiffUtil;
import eu.xenit.nuntio.engine.diff.InitialRegistrationResolver;
import eu.xenit.nuntio.engine.diff.RemoveService;
import eu.xenit.nuntio.engine.metrics.DiffOperationMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PlatformServicesSynchronizer {

    private final ServicePlatform platform;
    private final ServiceRegistry registry;
    private final DiffResolver diffResolver;
    private final InitialRegistrationResolver initialRegistrationResolver;
    private final DiffOperationMetrics syncMetrics;

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
