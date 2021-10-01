package be.vbgn.nuntio.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
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
    ConsulRegistry consulRegistry(ConsulClient consulClient, ConsulProperties consulProperties) {
        return new ConsulRegistry(consulClient, consulProperties);
    }
}
