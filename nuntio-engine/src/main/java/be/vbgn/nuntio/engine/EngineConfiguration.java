package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.AntiEntropyProperties;
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
            ServiceMapper serviceMapper, PlatformServicesRegistrar platformServicesRegistrar,
            EngineProperties engineProperties) {
        AntiEntropyProperties antiEntropyProperties = engineProperties.getAntiEntropy();
        if (!antiEntropyProperties.isEnabled()) {
            return null;
        }
        return new AntiEntropyDaemon(servicePlatform, serviceRegistry,
                platformServicesRegistrar, serviceMapper, antiEntropyProperties);
    }

    @Bean
    LiveWatchDaemon liveWatchDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry,
           ServiceMapper serviceMapper,
            EngineProperties engineProperties) {
        if (!engineProperties.getLive().isEnabled()) {
            return null;
        }
        return new LiveWatchDaemon(servicePlatform, serviceRegistry, serviceMapper, engineProperties.getLive());
    }

    @Bean
    PlatformServicesRegistrar platformServicesRegistrar(ServicePlatform servicePlatform,
            ServiceMapper serviceMapper) {
        return new PlatformServicesRegistrar(servicePlatform, serviceMapper);
    }

    @Bean
    ServiceMapper serviceMapper(ServiceRegistry serviceRegistry, EngineProperties engineProperties) {
        return new ServiceMapper(serviceRegistry, engineProperties);
    }
}
