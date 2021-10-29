package be.vbgn.nuntio.integration;

import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties;
import be.vbgn.nuntio.engine.LiveWatchDaemon;
import be.vbgn.nuntio.engine.PlatformServicesSynchronizer;
import be.vbgn.nuntio.integration.actuators.ShutdownEndpoint;
import be.vbgn.nuntio.integration.startup.NuntioApplicationNormalStartup;
import be.vbgn.nuntio.integration.startup.NuntioApplicationStartup;
import be.vbgn.nuntio.integration.startup.NuntioUnregisterAllStartup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
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
    NuntioApplicationNormalStartup normalStartup(PlatformServicesSynchronizer servicesRegistrar,
            ApplicationContext applicationContext) {
        return new NuntioApplicationNormalStartup(servicesRegistrar, applicationContext);
    }

    @Bean
    @ConditionalOnBean(LiveWatchDaemon.class)
    @ConditionalOnMissingBean(NuntioApplicationStartup.class)
    LiveWatchManager liveWatchManager(LiveWatchDaemon liveWatchDaemon) {
        return new LiveWatchManagerImpl(liveWatchDaemon);
    }

    @Bean
    @ConditionalOnMissingBean(LiveWatchManager.class)
    LiveWatchManager nullLiveWatchManager() {
        return new NullLiveWatchManager();
    }

    @Bean
    ShutdownEndpoint shutdownEndpoint(ServiceRegistry serviceRegistry, EngineProperties engineProperties, LiveWatchManager liveWatchManager, @Autowired(required = false) NuntioApplicationShutdown nuntioApplicationShutdown) {
        return new ShutdownEndpoint(serviceRegistry, engineProperties, liveWatchManager, nuntioApplicationShutdown);
    }
}
