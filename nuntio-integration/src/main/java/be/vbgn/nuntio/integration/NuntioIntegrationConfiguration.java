package be.vbgn.nuntio.integration;

import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties;
import be.vbgn.nuntio.engine.LiveWatchDaemon;
import be.vbgn.nuntio.engine.PlatformServicesSynchronizer;
import be.vbgn.nuntio.engine.availability.AvailabilityManager;
import be.vbgn.nuntio.integration.actuators.SetShutdownEndpoint;
import be.vbgn.nuntio.integration.availability.AvailabilityManagerImpl;
import be.vbgn.nuntio.integration.startup.NuntioApplicationNormalStartup;
import be.vbgn.nuntio.integration.startup.NuntioApplicationStartup;
import be.vbgn.nuntio.integration.startup.NuntioUnregisterAllStartup;
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
