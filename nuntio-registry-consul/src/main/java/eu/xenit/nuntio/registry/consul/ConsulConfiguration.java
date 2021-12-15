package eu.xenit.nuntio.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import eu.xenit.nuntio.api.checks.ServiceCheckFactory;
import eu.xenit.nuntio.api.registry.metrics.RegistryMetricsFactory;
import eu.xenit.nuntio.registry.consul.actuators.ConsulHealthIndicator;
import eu.xenit.nuntio.registry.consul.checks.ConsulCheckFactory;
import eu.xenit.nuntio.registry.consul.checks.ConsulHttpCheck.HttpCheckFactory;
import eu.xenit.nuntio.registry.consul.checks.ConsulTcpCheck.TcpCheckFactory;
import java.util.Arrays;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "nuntio.consul.enabled", matchIfMissing = true)
public class ConsulConfiguration {

    @Bean
    ConsulClientFactory consulClientFactory(ConsulProperties consulProperties) {
        return new ConsulClientFactory(consulProperties);
    }

    @Bean
    @ConfigurationProperties(value = "nuntio.consul", ignoreUnknownFields = false)
    ConsulProperties consulProperties() {
        return new ConsulProperties();
    }

    @Bean
    ConsulRegistry consulRegistry(ConsulClient consulClient, RegistryMetricsFactory metricsFactory, ConsulProperties consulProperties) {
        return new ConsulRegistry(consulClient, consulProperties, metricsFactory.createRegistryMetrics("consul"));
    }

    @Bean
    ServiceCheckFactory consulCheckFactory() {
        return new ConsulCheckFactory(Arrays.asList(
                new HttpCheckFactory(),
                new TcpCheckFactory()
        ));
    }

    @Bean
    HealthIndicator consulHealthIndicator(ConsulClient consulClient, ConsulProperties consulProperties) {
        return new ConsulHealthIndicator(consulClient, consulProperties);
    }

}
