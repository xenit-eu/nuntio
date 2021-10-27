package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.platform.docker.config.modifier.ServiceConfigurationModifier;
import be.vbgn.nuntio.platform.docker.config.parser.ContainerMetadata;
import be.vbgn.nuntio.platform.docker.config.parser.InspectContainerMetadata;
import be.vbgn.nuntio.platform.docker.config.parser.ServiceConfigurationParser;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class DockerContainerServiceDescriptionFactory {

    private final ServiceConfigurationParser configurationParser;
    private final List<ServiceConfigurationModifier> configurationModifiers;

    public PlatformServiceDescription createServiceDescription(InspectContainerResponse response) {
        return new DockerContainerServiceDescription(
                response,
                createConfiguration(response)
        );
    }

    private Set<PlatformServiceConfiguration> createConfiguration(InspectContainerResponse response) {
        return createConfiguration(new InspectContainerMetadata(response))
                .stream()
                .flatMap(configuration -> {
                    var optionalConfig = Stream.of(configuration);
                    for (ServiceConfigurationModifier modifier : configurationModifiers) {
                        optionalConfig = optionalConfig.flatMap(c -> modifier.modifyConfiguration(c, response));
                    }
                    return optionalConfig;
                })
                .collect(Collectors.toSet());
    }

    private Set<PlatformServiceConfiguration> createConfiguration(ContainerMetadata containerMetadata) {
        return configurationParser.toServiceConfigurations(containerMetadata);
    }
}
