package be.vbgn.nuntio.platform.docker.config.modifier;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
public class ExpandAnyBindingConfigurationModifier implements ServiceConfigurationModifier {

    @Override
    public Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse) {

        if (configuration.getServiceBinding() == ServiceBinding.ANY) {
            // We need exposedports here
            // * networksettings ports are cleared when the container is stopped
            // * exposed ports from "PORT" instruction + with "--expose" + "--publish" are in this list
            var exposedPorts = inspectContainerResponse.getConfig().getExposedPorts();
            if (exposedPorts == null) {
                log.warn("{} has any service binding, but has no exposed ports to map to.", configuration);
                return Stream.empty();
            }
            log.debug("Replacing {} with all exposed ports {}", configuration, Arrays.asList(exposedPorts));
            return Arrays.stream(exposedPorts)
                    .map(exposedPort -> ServiceBinding.fromPortAndProtocol(exposedPort.getPort(),
                            exposedPort.getProtocol().toString()))
                    .map(binding -> configuration.withBinding(binding));
        }
        return Stream.of(configuration);
    }

}
