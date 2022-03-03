package eu.xenit.nuntio.engine;

import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.ServicePlatform;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.EngineProperties.AntiEntropyProperties;
import eu.xenit.nuntio.engine.availability.AvailabilityManager;
import eu.xenit.nuntio.engine.diff.AddService;
import eu.xenit.nuntio.engine.diff.DiffResolver;
import eu.xenit.nuntio.engine.diff.DiffService;
import eu.xenit.nuntio.engine.diff.RemoveService;
import eu.xenit.nuntio.engine.metrics.DiffOperationMetrics;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;

@AllArgsConstructor
@Slf4j
public class AntiEntropyDaemon implements SchedulingConfigurer {

    private ServicePlatform platform;
    private ServiceRegistry registry;
    private DiffService diffService;
    private DiffResolver diffResolver;
    private DiffOperationMetrics antiEntropyMetrics;
    private AntiEntropyProperties antiEntropyProperties;
    private AvailabilityManager availabilityManager;

    public void runAntiEntropy() {
        try {
            log.debug("Running anti-entropy");

            Map<? extends RegistryServiceIdentifier, RegistryServiceDescription> registryServiceDescriptions = registry.findServiceDescriptions();
            Set<? extends PlatformServiceDescription> platformServiceDescriptions = platform.findAll();

            diffService.diff(registryServiceDescriptions, platformServiceDescriptions)
                    .peek(antiEntropyMetrics)
                    .peek(diff -> {
                        diff.cast(AddService.class).ifPresent(addService -> {
                            log.warn("Found platform {}, but registry is missing the {} service",
                                    addService.getDescription(), addService.getServiceConfiguration());
                        });
                        diff.cast(RemoveService.class).ifPresent(removeService -> {
                            log.warn("Found registered {}, but platform is no longer present", removeService.getRegistryServiceIdentifier());
                        });
                    })
                    .forEach(diffResolver);
            availabilityManager.registerSuccess(this);
        } catch (Throwable e) {
            log.error("Exception during anti-entropy run", e);
            antiEntropyMetrics.failure();
            availabilityManager.registerFailure(this);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        Duration delay = antiEntropyProperties.getDelay();
        PeriodicTrigger trigger = new PeriodicTrigger(delay.toMillis());
        trigger.setInitialDelay(delay.toMillis());
        taskRegistrar.addTriggerTask(() -> {
            if(!antiEntropyProperties.isEnabled())  {
                return;
            }
            runAntiEntropy();
        }, trigger);
    }
}
