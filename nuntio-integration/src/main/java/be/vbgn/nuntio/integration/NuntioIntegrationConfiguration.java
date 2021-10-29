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
    NuntioApplicationShutdown applicationShutdown(ServiceRegistry serviceRegistry) {
        return new NuntioApplicationShutdown(serviceRegistry);
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
            @Autowired(required = false) LiveWatchDaemon liveWatchDaemon, ApplicationContext applicationContext) {
        return new NuntioApplicationNormalStartup(servicesRegistrar, liveWatchDaemon, applicationContext);
    }

    @Bean
    @ConditionalOnBean(NuntioApplicationNormalStartup.class)
    ShutdownEndpoint shutdownEndpoint(ServiceRegistry serviceRegistry, EngineProperties engineProperties, NuntioApplicationNormalStartup nuntioApplicationNormalStartup, NuntioApplicationShutdown nuntioApplicationShutdown) {
        return new ShutdownEndpoint(serviceRegistry, engineProperties, nuntioApplicationNormalStartup, nuntioApplicationShutdown);
    }
}
