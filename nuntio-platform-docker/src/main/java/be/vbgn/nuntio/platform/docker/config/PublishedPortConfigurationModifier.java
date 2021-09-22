package be.vbgn.nuntio.platform.docker.config;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "nuntio.docker.bind", havingValue = "published")
@Slf4j
public class PublishedPortConfigurationModifier implements ServiceConfigurationModifier {

    @Override
    public Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse) {

        var portBindings = inspectContainerResponse.getNetworkSettings().getPorts().getBindings();
        log.debug("Replacing binding in {} with published ports {}", configuration, portBindings);
        ServiceBinding serviceBinding = configuration.getServiceBinding();
        var firstBinding = portBindings
                .entrySet()
                .stream()
                .filter(portBinding -> serviceBinding.getPort()
                        .map(port -> port.equals(Integer.toString(portBinding.getKey().getPort()))).orElse(true))
                .filter(portBinding -> serviceBinding.getProtocol()
                        .map(protocol -> protocol.equals(portBinding.getKey().getProtocol().toString())).orElse(true))
                .map(Entry::getValue)
                .filter(Objects::nonNull)
                .peek(bindings -> {
                    if (bindings.length > 1) {
                        log.warn("{} has multiple published ports {} for binding {}. Selecting one at-random.",
                                configuration,
                                Arrays.asList(bindings), configuration.getServiceBinding());
                    }
                })
                .flatMap(Arrays::stream)
                .findFirst();

        if (firstBinding.isEmpty()) {
            log.warn("{} has no published port mapping to {}.", configuration, configuration.getServiceBinding());
        }

        return firstBinding
                .map(binding -> configuration.withBinding(ServiceBinding.fromPortAndProtocol(binding.getHostPortSpec(),
                        configuration.getServiceBinding().getProtocol().orElse("tcp"))))
                .stream();
    }
}
