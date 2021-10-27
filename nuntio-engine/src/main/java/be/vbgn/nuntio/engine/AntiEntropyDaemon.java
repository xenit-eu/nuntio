package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.AntiEntropyProperties;
import java.time.Duration;
import java.util.HashSet;
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
    private PlatformServicesRegistrar platformServicesRegistrar;
    private ServiceMapper serviceMapper;
    private AntiEntropyProperties antiEntropyProperties;

    public void runAntiEntropy() {
        try {
            log.debug("Running anti-entropy");

            Set<PlatformServiceIdentifier> knownServices = new HashSet<>();
            // Remove services that have been published in any registry but have been removed from docker
            // Also updates healthchecks for services that still exist
            for (RegistryServiceIdentifier service : registry.findServices()) {
                var platformService = platform.find(service.getPlatformIdentifier());
                platformService.map(PlatformServiceDescription::getIdentifier).ifPresent(knownServices::add);
                platformService.ifPresentOrElse(platformServiceDescription -> {
                    if(platformServiceDescription.getState() == PlatformServiceState.STOPPED) {
                        serviceMapper.unregisterService(platformServiceDescription);
                    } else {
                        serviceMapper.updateServiceChecks(platformServiceDescription);
                    }
                }, () -> {
                    log.info("Found registered {}, but platform is no longer present", service);
                    registry.unregisterService(service);
                });
            }

            for(PlatformServiceDescription platformServiceDescription: platform.findAll()) {
                if(!knownServices.contains(platformServiceDescription.getIdentifier())) {
                    log.info("Found platform {}, but registry does not have a service for it.", platformServiceDescription);
                    serviceMapper.registerService(platformServiceDescription);
                }
            }

            platformServicesRegistrar.registerAllServices();
        } catch (Throwable e) {
            log.error("Exception during anti-entropy run", e);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        Duration delay = antiEntropyProperties.getDelay();
        PeriodicTrigger trigger = new PeriodicTrigger(delay.toMillis());
        trigger.setInitialDelay(delay.toMillis());
        taskRegistrar.addTriggerTask(this::runAntiEntropy, trigger);
    }
}
