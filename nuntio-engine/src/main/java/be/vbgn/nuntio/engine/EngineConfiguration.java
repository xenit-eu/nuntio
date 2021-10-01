package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.AntiEntropyProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class EngineConfiguration implements SchedulingConfigurer {

    @Bean
    @ConfigurationProperties(value = "nuntio.engine", ignoreUnknownFields = false)
    EngineProperties engineProperties() {
        return new EngineProperties();
    }

    @Bean
    AntiEntropyDaemon antiEntropyDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry,
            ChecksProcessor checksProcessor, PlatformServicesRegistrar platformServicesRegistrar,
            EngineProperties engineProperties) {
        AntiEntropyProperties antiEntropyProperties = engineProperties.getAntiEntropy();
        if (!antiEntropyProperties.isEnabled()) {
            return null;
        }
        return new AntiEntropyDaemon(servicePlatform, serviceRegistry, checksProcessor,
                platformServicesRegistrar, antiEntropyProperties);
    }

    @Bean
    ChecksProcessor checksProcessor(ServiceRegistry serviceRegistry, EngineProperties engineProperties) {
        return new ChecksProcessor(serviceRegistry, engineProperties.getChecks());
    }

    @Bean
    LiveWatchDaemon liveWatchDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry,
            ChecksProcessor checksProcessor, PlatformServicesRegistrar platformServicesRegistrar,
            EngineProperties engineProperties) {
        if (!engineProperties.getLive().isEnabled()) {
            return null;
        }
        return new LiveWatchDaemon(servicePlatform, serviceRegistry, checksProcessor, platformServicesRegistrar,
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


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

    }
}
