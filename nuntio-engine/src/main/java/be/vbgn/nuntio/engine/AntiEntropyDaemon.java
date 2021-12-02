package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.AntiEntropyProperties;
import be.vbgn.nuntio.engine.diff.AddService;
import be.vbgn.nuntio.engine.diff.DiffResolver;
import be.vbgn.nuntio.engine.diff.DiffUtil;
import be.vbgn.nuntio.engine.diff.RemoveService;
import be.vbgn.nuntio.engine.metrics.OperationMetrics;
import java.time.Duration;
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
    private DiffResolver diffResolver;
    private OperationMetrics antiEntropyMetrics;
    private AntiEntropyProperties antiEntropyProperties;

    public void runAntiEntropy() {
        try {
            log.debug("Running anti-entropy");

            Set<? extends RegistryServiceIdentifier> registryServiceIdentifiers = registry.findServices();
            Set<? extends PlatformServiceDescription> platformServiceDescriptions = platform.findAll();

            DiffUtil.diff(registryServiceIdentifiers, platformServiceDescriptions)
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
        } catch (Throwable e) {
            log.error("Exception during anti-entropy run", e);
            antiEntropyMetrics.failure();
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
