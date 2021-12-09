package eu.xenit.nuntio.engine;

import eu.xenit.nuntio.api.platform.ServicePlatform;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.EngineProperties.AntiEntropyProperties;
import eu.xenit.nuntio.engine.availability.AvailabilityManager;
import eu.xenit.nuntio.engine.diff.DiffResolver;
import eu.xenit.nuntio.engine.diff.InitialRegistrationResolver;
import eu.xenit.nuntio.engine.metrics.LiveWatchMetrics;
import eu.xenit.nuntio.engine.metrics.DiffOperationMetrics;
import eu.xenit.nuntio.engine.metrics.MetricsFactory;
import io.micrometer.core.instrument.MeterRegistry;
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
    AntiEntropyDaemon antiEntropyDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffResolver diffResolver, MeterRegistry meterRegistry, EngineProperties engineProperties, AvailabilityManager availabilityManager) {
        AntiEntropyProperties antiEntropyProperties = engineProperties.getAntiEntropy();
        return new AntiEntropyDaemon(servicePlatform, serviceRegistry, diffResolver, new DiffOperationMetrics(meterRegistry, "anti-entropy"), antiEntropyProperties, availabilityManager);
    }

    @Bean
    LiveWatchDaemon liveWatchDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffResolver diffResolver, MeterRegistry meterRegistry, EngineProperties engineProperties, AvailabilityManager availabilityManager) {
        return new LiveWatchDaemon(servicePlatform, serviceRegistry, diffResolver, new LiveWatchMetrics(meterRegistry), engineProperties.getLive(), availabilityManager);
    }

    @Bean
    PlatformServicesSynchronizer platformServicesRegistrar(ServicePlatform servicePlatform,
            ServiceRegistry registry,
            DiffResolver diffResolver,
            InitialRegistrationResolver initialRegistrationResolver,
            MeterRegistry meterRegistry
            ) {
        return new PlatformServicesSynchronizer(servicePlatform, registry, diffResolver, initialRegistrationResolver, new DiffOperationMetrics(meterRegistry, "sync"));
    }

    @Bean
    DiffResolver diffResolver(ServiceRegistry registry, EngineProperties engineProperties)  {
        return new DiffResolver(registry, engineProperties);
    }

    @Bean
    InitialRegistrationResolver initialRegistrationResolver(ServiceRegistry registry, EngineProperties engineProperties) {
        return new InitialRegistrationResolver(registry, engineProperties);
    }

    @Bean
    MetricsFactory metricsFactory(MeterRegistry meterRegistry) {
        return new MetricsFactory(meterRegistry);
    }
}
