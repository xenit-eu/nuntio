package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.platform.metrics.PlatformMetricsFactory;
import be.vbgn.nuntio.platform.docker.DockerProperties.PortBindConfiguration;
import be.vbgn.nuntio.platform.docker.actuators.DockerHealthIndicator;
import be.vbgn.nuntio.platform.docker.config.modifier.ExpandAnyBindingConfigurationModifier;
import be.vbgn.nuntio.platform.docker.config.modifier.InternalNetworkConfigurationModifier;
import be.vbgn.nuntio.platform.docker.config.modifier.PublishedPortConfigurationModifier;
import be.vbgn.nuntio.platform.docker.config.modifier.ServiceConfigurationModifier;
import be.vbgn.nuntio.platform.docker.config.parser.NullServiceConfigurationParser;
import be.vbgn.nuntio.platform.docker.config.parser.NuntioLabelsParser;
import be.vbgn.nuntio.platform.docker.config.parser.RegistratorCompatibleParser;
import be.vbgn.nuntio.platform.docker.config.parser.ServiceConfigurationParser;
import be.vbgn.nuntio.platform.docker.config.parser.SwitchingServiceConfigurationParser;
import com.github.dockerjava.api.DockerClient;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnProperty(value = "nuntio.docker.enabled", matchIfMissing = true)
public class DockerConfiguration {

    @Bean
    @ConfigurationProperties(value = "nuntio.docker", ignoreUnknownFields = false)
    DockerProperties dockerProperties() {
        return new DockerProperties();
    }

    @Bean
    @Lazy
    DockerClientFactory dockerClientFactory(DockerProperties dockerProperties) {
        return new DockerClientFactory(dockerProperties);
    }

    @Bean
    DockerContainerServiceDescriptionFactory serviceDescriptionFactory(ServiceConfigurationParser configurationParser,
            List<ServiceConfigurationModifier> configurationModifiers) {
        return new DockerContainerServiceDescriptionFactory(configurationParser, configurationModifiers);
    }

    @Bean
    @Lazy
    DockerContainerWatcher containerWatcher(DockerClient dockerClient) {
        return new DockerContainerWatcher(dockerClient);
    }

    @Bean
    DockerPlatform dockerPlatform(DockerClient dockerClient,
            DockerContainerServiceDescriptionFactory serviceDescriptionFactory,
            @Lazy DockerContainerWatcher containerWatcher,
            DockerPlatformEventFactory eventFactory,
            PlatformMetricsFactory metricsFactory
            ) {
        return new DockerPlatform(dockerClient, serviceDescriptionFactory, containerWatcher, eventFactory, metricsFactory.createPlatformMetrics("docker"));
    }

    @Bean
    DockerPlatformEventFactory dockerPlatformEventFactory() {
        return new DockerPlatformEventFactory();
    }

    @Bean
    ServiceConfigurationParser nuntioLabelsParser(DockerProperties dockerProperties) {
        if(dockerProperties.getNuntioLabel().isEnabled()) {
            return new NuntioLabelsParser(dockerProperties.getNuntioLabel().getPrefix());
        }
        return new NullServiceConfigurationParser();
    }

    @Bean
    ServiceConfigurationParser registratorCompatibleParser(DockerProperties dockerProperties) {
        if(dockerProperties.getRegistratorCompat().isEnabled()) {
            return new RegistratorCompatibleParser(dockerProperties.getRegistratorCompat());
        }
        return new NullServiceConfigurationParser();
    }

    @Bean
    @Primary
    ServiceConfigurationParser switchingServiceConfigurationParser(DockerProperties dockerProperties) {
        return new SwitchingServiceConfigurationParser(Arrays.asList(
                nuntioLabelsParser(dockerProperties),
                registratorCompatibleParser(dockerProperties)
        ));
    }

    @Bean
    @Order(1)
    ServiceConfigurationModifier expandAnyBindingConfigurationModifier(DockerProperties dockerProperties) {
        return new ExpandAnyBindingConfigurationModifier();
    }

    @Bean
    ServiceConfigurationModifier internalNetworkConfigurationModifier(DockerProperties dockerProperties, DockerClient dockerClient, @Value("${nuntio.docker.bind.filter:}") String networkFilter) {
        if(dockerProperties.getBind() == PortBindConfiguration.INTERNAL) {
            return new InternalNetworkConfigurationModifier(dockerClient, networkFilter);
        }
        return null;
    }

    @Bean
    ServiceConfigurationModifier publishedPortConfigurationModifier(DockerProperties dockerProperties) {
        if(dockerProperties.getBind() == PortBindConfiguration.PUBLISHED) {
            return new PublishedPortConfigurationModifier();
        }
        return null;
    }

    @Bean
    HealthIndicator dockerHealthIndicator(DockerClient dockerClient) {
        return new DockerHealthIndicator(dockerClient);
    }

}
