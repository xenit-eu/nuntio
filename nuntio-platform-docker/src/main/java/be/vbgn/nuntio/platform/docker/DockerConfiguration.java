package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.platform.docker.config.modifier.ServiceConfigurationModifier;
import be.vbgn.nuntio.platform.docker.config.parser.NuntioLabelsParser;
import be.vbgn.nuntio.platform.docker.config.parser.ServiceConfigurationParser;
import com.github.dockerjava.api.DockerClient;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

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
            DockerPlatformEventFactory eventFactory) {
        return new DockerPlatform(dockerClient, serviceDescriptionFactory, containerWatcher, eventFactory);
    }

    @Bean
    DockerPlatformEventFactory dockerPlatformEventFactory() {
        return new DockerPlatformEventFactory();
    }

    @Bean
    NuntioLabelsParser nuntioLabelsParser(DockerProperties dockerProperties) {
        if(dockerProperties.getNuntioLabel().isEnabled()) {
            return new NuntioLabelsParser(dockerProperties.getNuntioLabel().getPrefix());
        }
        return null;
    }


}
