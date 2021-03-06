package eu.xenit.nuntio.engine;

import eu.xenit.nuntio.api.platform.ServicePlatform;
import eu.xenit.nuntio.api.postprocessor.PlatformServicePostProcessor;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.EngineProperties.AntiEntropyProperties;
import eu.xenit.nuntio.engine.availability.AvailabilityManager;
import eu.xenit.nuntio.engine.diff.DiffResolver;
import eu.xenit.nuntio.engine.diff.DiffService;
import eu.xenit.nuntio.engine.diff.InitialRegistrationResolver;
import eu.xenit.nuntio.engine.mapper.PlatformToRegistryMapper;
import eu.xenit.nuntio.engine.metrics.LiveWatchMetrics;
import eu.xenit.nuntio.engine.metrics.DiffOperationMetrics;
import eu.xenit.nuntio.engine.metrics.MetricsFactory;
import eu.xenit.nuntio.engine.postprocessor.ForceTagsPostProcessor;
import eu.xenit.nuntio.engine.postprocessor.RemoveDisabledAddressFamiliesPostProcessor;
import eu.xenit.nuntio.engine.postprocessor.RemoveStoppedPlatformsPostProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
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
    AntiEntropyDaemon antiEntropyDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffService diffService, DiffResolver diffResolver, MeterRegistry meterRegistry, EngineProperties engineProperties, AvailabilityManager availabilityManager) {
        AntiEntropyProperties antiEntropyProperties = engineProperties.getAntiEntropy();
        return new AntiEntropyDaemon(servicePlatform, serviceRegistry, diffService, diffResolver, new DiffOperationMetrics(meterRegistry, "anti-entropy"), antiEntropyProperties, availabilityManager);
    }

    @Bean
    LiveWatchDaemon liveWatchDaemon(ServicePlatform servicePlatform, ServiceRegistry serviceRegistry, DiffService diffService, DiffResolver diffResolver, MeterRegistry meterRegistry, EngineProperties engineProperties, AvailabilityManager availabilityManager) {
        return new LiveWatchDaemon(servicePlatform, serviceRegistry, diffService, diffResolver, new LiveWatchMetrics(meterRegistry), engineProperties.getLive(), availabilityManager);
    }

    @Bean
    PlatformServicesSynchronizer platformServicesRegistrar(ServicePlatform servicePlatform,
            ServiceRegistry registry,
            DiffService diffService,
            DiffResolver diffResolver,
            InitialRegistrationResolver initialRegistrationResolver,
            MeterRegistry meterRegistry
            ) {
        return new PlatformServicesSynchronizer(servicePlatform, registry, diffService, diffResolver, initialRegistrationResolver, new DiffOperationMetrics(meterRegistry, "sync"));
    }

    @Bean
    PlatformServicePostProcessor removeStoppedPlatformsPostProcessor() {
        return new RemoveStoppedPlatformsPostProcessor();
    }

    @Bean
    PlatformServicePostProcessor removeDisabledAddressFamiliesPostProcessor(EngineProperties engineProperties) {
        return new RemoveDisabledAddressFamiliesPostProcessor(engineProperties.getServiceAddress());
    }

    @Bean
    PlatformServicePostProcessor forceTagsPostProcessor(EngineProperties engineProperties) {
        return new ForceTagsPostProcessor(engineProperties.getForcedTags());
    }

    @Bean
    PlatformToRegistryMapper platformToRegistryMapper() {
        return new PlatformToRegistryMapper();
    }

    @Bean
    DiffService diffService(PlatformToRegistryMapper platformToRegistryMapper, List<PlatformServicePostProcessor> platformServicePostProcessorList) {
        return new DiffService(platformToRegistryMapper, platformServicePostProcessorList);
    }

    @Bean
    DiffResolver diffResolver(ServiceRegistry registry, EngineProperties engineProperties, PlatformToRegistryMapper platformToRegistryMapper)  {
        return new DiffResolver(registry, engineProperties, platformToRegistryMapper);
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
