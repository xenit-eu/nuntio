package be.vbgn.nuntio.platform.docker.config.modifier;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
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
        if (inspectContainerResponse.getState().getPidLong() == 0) {
            // Non-running containers do not have host port bindings, so we can't map them to internal IP addresses
            // Luckily, we don't need these IPs when the container is not running,
            // because non-running containers are only ever *deregistered* as a service
            log.debug("{} is not running, not adding port mappings", configuration);
            return Stream.of(configuration);
        }

        var portBindings = inspectContainerResponse.getNetworkSettings().getPorts().getBindings();
        if (log.isDebugEnabled()) {
            var logPortBindings = portBindings.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Entry::getKey,
                            entry -> entry.getValue() == null ? Collections.emptyList()
                                    : Arrays.asList(entry.getValue())));

            log.debug("Replacing binding in {} with published ports {}", configuration, logPortBindings);
        }
        ServiceBinding serviceBinding = configuration.getServiceBinding();
        var allBindings = portBindings
                .entrySet()
                .stream()
                .filter(portBinding -> serviceBinding.getPort()
                        .map(port -> port.equals(Integer.toString(portBinding.getKey().getPort()))).orElse(true))
                .filter(portBinding -> serviceBinding.getProtocol()
                        .map(protocol -> protocol.equals(portBinding.getKey().getProtocol().toString())).orElse(true))
                .map(Entry::getValue)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .map(binding -> {
                    ServiceBinding publishedServiceBinding = ServiceBinding.fromPortAndProtocol(
                            binding.getHostPortSpec(), configuration.getServiceBinding().getProtocol().orElse("tcp"));
                    if (hasUsefulIp(binding.getHostIp())) {
                        return publishedServiceBinding.withIp(binding.getHostIp());
                    }
                    return publishedServiceBinding;
                })
                .collect(Collectors.toSet());

        if (allBindings.isEmpty()) {
            log.warn("{} has no published port mapping to {}.", configuration, configuration.getServiceBinding());
        } else if (allBindings.size() > 1) {
            log.warn("{} has multiple published port mappings to {}. Created bindings {}", configuration,
                    configuration.getServiceBinding(),
                    allBindings);
        }

        return allBindings
                .stream()
                .map(binding -> configuration.withBinding(binding)
                        .withInternalMetadata("docker-internal-port", serviceBinding.toString()));
    }


    private static boolean hasUsefulIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return !address.isAnyLocalAddress();
        } catch (UnknownHostException e) {
            return true;
        }
    }
}
