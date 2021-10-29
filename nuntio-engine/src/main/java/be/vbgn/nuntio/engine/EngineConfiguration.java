package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.AntiEntropyProperties;
import be.vbgn.nuntio.engine.diff.DiffResolver;
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
    AntiEntropyDaemon antiEntropyDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffResolver diffResolver, EngineProperties engineProperties) {
        AntiEntropyProperties antiEntropyProperties = engineProperties.getAntiEntropy();
        if (!antiEntropyProperties.isEnabled()) {
            return null;
        }
        return new AntiEntropyDaemon(servicePlatform, serviceRegistry, diffResolver, antiEntropyProperties);
    }

    @Bean
    LiveWatchDaemon liveWatchDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffResolver diffResolver, EngineProperties engineProperties) {
        if (!engineProperties.getLive().isEnabled()) {
            return null;
        }
        return new LiveWatchDaemon(servicePlatform, serviceRegistry, diffResolver, engineProperties.getLive());
    }

    @Bean
    PlatformServicesRegistrar platformServicesRegistrar(ServicePlatform servicePlatform,
            ServiceRegistry registry,
            DiffResolver diffResolver) {
        return new PlatformServicesRegistrar(servicePlatform, registry, diffResolver);
    }

    @Bean
    DiffResolver diffResolver(ServiceRegistry registry, EngineProperties engineProperties)  {
        return new DiffResolver(registry, engineProperties);
    }
}
