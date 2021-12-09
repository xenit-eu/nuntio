package eu.xenit.nuntio.integration;

import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.EngineProperties;
import eu.xenit.nuntio.engine.LiveWatchDaemon;
import eu.xenit.nuntio.engine.PlatformServicesSynchronizer;
import eu.xenit.nuntio.engine.availability.AvailabilityManager;
import eu.xenit.nuntio.integration.actuators.SetShutdownEndpoint;
import eu.xenit.nuntio.integration.availability.AvailabilityManagerImpl;
import eu.xenit.nuntio.integration.startup.NuntioApplicationNormalStartup;
import eu.xenit.nuntio.integration.startup.NuntioApplicationStartup;
import eu.xenit.nuntio.integration.startup.NuntioUnregisterAllStartup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NuntioIntegrationConfiguration {


    @Bean
    @ConditionalOnBean(NuntioApplicationNormalStartup.class)
    NuntioApplicationShutdown applicationShutdown(ServiceRegistry serviceRegistry, LiveWatchManager liveWatchManager, EngineProperties engineProperties) {
        return new NuntioApplicationShutdown(serviceRegistry, liveWatchManager, engineProperties);
    }

    @Bean
    @ConditionalOnProperty("nuntio.app.unregister-all")
    NuntioUnregisterAllStartup unregisterAllStartup(ServiceRegistry serviceRegistry,
            ConfigurableApplicationContext applicationContext) {
        return new NuntioUnregisterAllStartup(serviceRegistry, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(NuntioApplicationStartup.class)
    NuntioApplicationNormalStartup normalStartup(PlatformServicesSynchronizer servicesRegistrar) {
        return new NuntioApplicationNormalStartup(servicesRegistrar);
    }

    @Bean
    @ConditionalOnMissingBean(NuntioApplicationStartup.class)
    LiveWatchManager liveWatchManager(LiveWatchDaemon liveWatchDaemon, EngineProperties engineProperties) {
        if(engineProperties.getLive().isEnabled()) {
            return new LiveWatchManagerImpl(liveWatchDaemon);
        } else {
            return new NullLiveWatchManager();
        }
    }

    @Bean
    SetShutdownEndpoint setShutdownEndpoint(EngineProperties engineProperties) {
        return new SetShutdownEndpoint(engineProperties);
    }

    @Bean
    AvailabilityManager availabilityManager(ApplicationEventPublisher eventPublisher) {
        return new AvailabilityManagerImpl(eventPublisher);
    }
}
