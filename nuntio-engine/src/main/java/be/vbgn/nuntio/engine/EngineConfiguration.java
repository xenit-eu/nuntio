package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.AntiEntropyProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

@Configuration
public class EngineConfiguration {

    @Bean
    @ConfigurationProperties(value = "nuntio.engine", ignoreUnknownFields = false)
    EngineProperties engineProperties() {
        return new EngineProperties();
    }

    @Bean
    AntiEntropyDaemon antiEntropyDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry,
            ChecksProcessor checksProcessor, PlatformServicesRegistrar platformServicesRegistrar,
            EngineProperties engineProperties, TaskScheduler taskScheduler) {
        AntiEntropyProperties antiEntropyProperties = engineProperties.getAntiEntropy();
        if (!antiEntropyProperties.isEnabled()) {
            return null;
        }
        AntiEntropyDaemon antiEntropyDaemon = new AntiEntropyDaemon(servicePlatform, serviceRegistry, checksProcessor,
                platformServicesRegistrar);
        // Schedule runs
        Duration delay = antiEntropyProperties.getDelay();
        PeriodicTrigger trigger = new PeriodicTrigger(delay.toMillis());
        trigger.setInitialDelay(delay.toMillis());
        taskScheduler.schedule(antiEntropyDaemon::runAntiEntropy, trigger);
        return antiEntropyDaemon;
    }

    @Bean
    ChecksProcessor checksProcessor(ServiceRegistry serviceRegistry) {
        return new ChecksProcessor(serviceRegistry);
    }

    @Bean
    LiveWatchDaemon liveWatchDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry,
            ChecksProcessor checksProcessor, PlatformToRegistryMapper platformToRegistryMapper,
            EngineProperties engineProperties) {
        if (!engineProperties.getLive().isEnabled()) {
            return null;
        }
        return new LiveWatchDaemon(servicePlatform, serviceRegistry, platformToRegistryMapper, checksProcessor,
                engineProperties.getLive());
    }

    @Bean
    PlatformServicesRegistrar platformServicesRegistrar(ServicePlatform servicePlatform,
            ServiceRegistry serviceRegistry, PlatformToRegistryMapper platformToRegistryMapper,
            ChecksProcessor checksProcessor) {
        return new PlatformServicesRegistrar(servicePlatform, serviceRegistry, platformToRegistryMapper,
                checksProcessor);
    }

    @Bean
    PlatformToRegistryMapper platformToRegistryMapper() {
        return new PlatformToRegistryMapper();
    }


}
