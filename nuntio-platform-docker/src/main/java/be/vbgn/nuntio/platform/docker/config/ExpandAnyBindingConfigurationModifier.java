package be.vbgn.nuntio.platform.docker.config;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.DockerSharedIdentifier;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Slf4j
public class ExpandAnyBindingConfigurationModifier implements ServiceConfigurationModifier {

    @Override
    public Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse) {
        var exposedPorts = inspectContainerResponse.getNetworkSettings().getPorts().getBindings();

        if (configuration.getServiceBinding() == ServiceBinding.ANY) {
            log.debug("Replacing {} with all exposed ports {}", configuration, exposedPorts.keySet());
            return exposedPorts.keySet().stream()
                    .map(exposedPort -> ServiceBinding.fromPortAndProtocol(exposedPort.getPort(),
                            exposedPort.getProtocol().toString()))
                    .map(binding -> configuration
                            .withBinding(binding)
                            .withIdentifier(
                                    DockerSharedIdentifier.fromContainerIdAndPort(inspectContainerResponse.getId(),
                                            binding.getPort()))
                    );
        }
        return Stream.of(configuration);
    }

}
