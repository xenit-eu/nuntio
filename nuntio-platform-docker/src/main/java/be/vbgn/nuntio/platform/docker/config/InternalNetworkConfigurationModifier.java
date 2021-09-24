package be.vbgn.nuntio.platform.docker.config;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "nuntio.docker.bind", havingValue = "internal")
@ConditionalOnBean(DockerClient.class)
@Slf4j
public class InternalNetworkConfigurationModifier implements ServiceConfigurationModifier {

    private final DockerClient dockerClient;


    private final String networkFilter;

    private volatile Map<String, Boolean> networkNamesCache;

    public InternalNetworkConfigurationModifier(@Autowired DockerClient dockerClient,
            @Value("${nuntio.docker.bind.filter:}") String networkFilter) {
        this.dockerClient = dockerClient;
        this.networkFilter = networkFilter;
    }

    private Map<String, Set<String>> createFilters() {
        return Arrays.stream(networkFilter.split(","))
                .filter(Predicate.not(String::isEmpty))
                .map(filterSpec -> filterSpec.split("=", 2))
                .collect(Collectors.toMap(part -> part[0], part -> Collections.singleton(part[1]), (setA, setB) -> {
                    Set<String> merged = new HashSet<>(setA);
                    merged.addAll(setB);
                    return merged;
                }));
    }

    private synchronized void updateNetworkNames() {
        Map<String, Set<String>> filters = createFilters();
        log.info("Updating list of network names (filtered by {})", filters);

        var networksCmd = dockerClient.listNetworksCmd();

        var networkNames = networksCmd.exec()
                .stream()
                .flatMap(network -> Stream.of(network.getName(), network.getId()))
                .collect(Collectors.toMap(Function.identity(), _network -> false));

        filters.forEach(networksCmd::withFilter);

        networksCmd.exec()
                .forEach(network -> {
                    networkNames.put(network.getName(), true);
                    networkNames.put(network.getId(), true);
                });

        log.debug("Networks matching filters: {}", networkNames);

        this.networkNamesCache = networkNames;
    }

    private boolean filterNetwork(String networkName) {
        if (networkNamesCache == null || !networkNamesCache.containsKey(networkName)) {
            updateNetworkNames();
        }
        return networkNamesCache.getOrDefault(networkName, false);
    }


    @Override
    public Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse) {

        if (inspectContainerResponse.getState().getPidLong() != 0) {
            // Non-running containers do not have network IPs, so we can't map them to internal IP addresses
            // Luckily, we don't need these IPs when the container is not running,
            // because non-running containers are only ever *deregistered* as a service
            log.debug("{} is not running, not adding port mappings", configuration);
            return Stream.of(configuration);
        }

        var containerNetworks = Optional.ofNullable(inspectContainerResponse)
                .map(InspectContainerResponse::getNetworkSettings)
                .map(NetworkSettings::getNetworks)
                .map(Map::entrySet)
                .stream()
                .flatMap(Collection::stream)
                .filter(network -> filterNetwork(network.getKey()))
                .collect(Collectors.toSet());

        switch (containerNetworks.size()) {
            case 0:
                log.warn("{} has no internal networks to bind to.", configuration);
                return Stream.empty();
            default:
                if (log.isWarnEnabled()) {
                    log.warn("{} has multiple internal networks {} for binding {}. Selecting one at-random.",
                            configuration,
                            containerNetworks.stream().map(Entry::getKey).collect(
                                    Collectors.toSet()), configuration.getServiceBinding());
                }
                // no break
            case 1:
                return containerNetworks.stream()
                        .findAny()
                        .map(Entry::getValue)
                        .map(ContainerNetwork::getIpAddress)
                        .map(internalIp -> {
                            log.debug("Replacing binding in {} with internal IP {}", configuration, internalIp);
                            ServiceBinding serviceBinding = configuration.getServiceBinding();
                            return configuration.withBinding(serviceBinding.withIp(internalIp));
                        })
                        .stream();
        }
    }

}
