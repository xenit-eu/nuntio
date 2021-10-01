package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            EngineProperties engineProperties) {
        if (!engineProperties.getAntiEntropy().isEnabled()) {
            return null;
        }
        return new AntiEntropyDaemon(servicePlatform, serviceRegistry, checksProcessor, platformServicesRegistrar);
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
