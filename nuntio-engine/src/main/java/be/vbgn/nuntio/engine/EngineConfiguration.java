package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties.AntiEntropyProperties;
import be.vbgn.nuntio.engine.diff.DiffResolver;
import be.vbgn.nuntio.engine.diff.InitialRegistrationResolver;
import be.vbgn.nuntio.engine.metrics.LiveWatchMetrics;
import be.vbgn.nuntio.engine.metrics.OperationMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    AntiEntropyDaemon antiEntropyDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffResolver diffResolver, MeterRegistry meterRegistry, EngineProperties engineProperties) {
        AntiEntropyProperties antiEntropyProperties = engineProperties.getAntiEntropy();
        return new AntiEntropyDaemon(servicePlatform, serviceRegistry, diffResolver, new OperationMetrics(meterRegistry, "anti-entropy"), antiEntropyProperties);
    }

    @Bean
    LiveWatchDaemon liveWatchDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffResolver diffResolver, MeterRegistry meterRegistry, EngineProperties engineProperties) {
        return new LiveWatchDaemon(servicePlatform, serviceRegistry, diffResolver, new LiveWatchMetrics(meterRegistry), engineProperties.getLive());
    }

    @Bean
    PlatformServicesSynchronizer platformServicesRegistrar(ServicePlatform servicePlatform,
            ServiceRegistry registry,
            DiffResolver diffResolver,
            InitialRegistrationResolver initialRegistrationResolver,
            MeterRegistry meterRegistry
            ) {
        return new PlatformServicesSynchronizer(servicePlatform, registry, diffResolver, initialRegistrationResolver, new OperationMetrics(meterRegistry, "sync"));
    }

    @Bean
    DiffResolver diffResolver(ServiceRegistry registry, EngineProperties engineProperties)  {
        return new DiffResolver(registry, engineProperties);
    }

    @Bean
    InitialRegistrationResolver initialRegistrationResolver(ServiceRegistry registry, EngineProperties engineProperties) {
        return new InitialRegistrationResolver(registry, engineProperties);
    }
}
