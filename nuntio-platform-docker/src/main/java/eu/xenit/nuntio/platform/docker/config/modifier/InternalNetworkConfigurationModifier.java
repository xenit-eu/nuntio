package eu.xenit.nuntio.platform.docker.config.modifier;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.NetworkSettings;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "nuntio.docker.bind", havingValue = "internal")
@ConditionalOnBean(DockerClient.class)
@Slf4j
public class InternalNetworkConfigurationModifier implements ServiceConfigurationModifier {

    @FunctionalInterface
    public interface DockerNetworksFetcher {

        Stream<String> getNetworkIdsMatching(Map<String, Set<String>> filter);
    }

    private final DockerNetworksFetcher networksFetcher;

    private final Map<String, Set<String>> networkFilter;

    private volatile Map<String, Boolean> networkNamesCache;
    private final Object networkNamesCacheLock = new Object();

    public InternalNetworkConfigurationModifier(@Autowired DockerClient dockerClient,
             String networkFilter) {
        this(filter -> {
            var networksCmd = dockerClient.listNetworksCmd();
            filter.forEach(networksCmd::withFilter);
            return networksCmd.exec().stream().map(Network::getId);
        }, networkFilter);
    }

    public InternalNetworkConfigurationModifier(DockerNetworksFetcher networksFetcher, String networkFilter) {
        this.networksFetcher = networksFetcher;
        this.networkFilter = createFilters(networkFilter);
    }

    private static Map<String, Set<String>> createFilters(String networkFilter) {
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
        log.info("Updating list of network names (filtered by {})", networkFilter);

        // Fill cache with all network names & ids, mapping to "not matching"
        var networkNames = networksFetcher.getNetworkIdsMatching(Collections.emptyMap())
                .collect(Collectors.toMap(Function.identity(), _network -> false));

        // Execute command again with the filters to apply
        // Now map all names & ids that we got by filtering to "matching"
        networksFetcher.getNetworkIdsMatching(networkFilter)
                .forEach(network -> {
                    networkNames.put(network, true);
                });

        // We now have a map of network names & ids to a 'matches filter' boolean

        log.debug("Networks matching filters: {}", networkNames);

        synchronized (networkNamesCacheLock) {
            this.networkNamesCache = networkNames;
        }
    }

    private boolean filterNetwork(String networkName) {
        if (networkNamesCache == null || !networkNamesCache.containsKey(networkName)) {
            synchronized (networkNamesCacheLock) {
                if (networkNamesCache == null || !networkNamesCache.containsKey(networkName)) {
                    updateNetworkNames();
                }
            }
        }
        return networkNamesCache.getOrDefault(networkName, false);
    }


    @Override
    public Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse) {

        if (inspectContainerResponse.getState().getPidLong() == 0) {
            // Non-running containers do not have network IPs, so we can't map them to internal IP addresses
            // Luckily, we don't need these IPs when the container is not running,
            // because non-running containers are only ever *deregistered* as a service
            log.debug("{} is not running, not adding port mappings", configuration);
            return Stream.of(configuration);
        }

        var containerNetworks = Optional.of(inspectContainerResponse)
                .map(InspectContainerResponse::getNetworkSettings)
                .map(NetworkSettings::getNetworks)
                .map(Map::values)
                .stream()
                .flatMap(Collection::stream)
                .filter(network -> filterNetwork(network.getNetworkID()))
                .collect(Collectors.toSet());

        switch (containerNetworks.size()) {
            case 0:
                log.warn("{} has no internal networks to bind to.", configuration);
                return Stream.empty();
            case 1:
                return containerNetworks.stream()
                        .findAny()
                        .map(ContainerNetwork::getIpAddress)
                        .map(internalIp -> {
                            log.debug("Replacing binding in {} with internal IP {}", configuration, internalIp);
                            ServiceBinding serviceBinding = configuration.getServiceBinding();
                            return configuration.withBinding(serviceBinding.withIp(internalIp));
                        })
                        .stream();
            default:
                log.warn(
                        "{} has multiple internal networks {} for binding {}. Refusing to select any internal network.",
                        configuration,
                        containerNetworks.stream().map(ContainerNetwork::getNetworkID).collect(Collectors.toSet()),
                        configuration.getServiceBinding()
                );
                return Stream.empty();
        }
    }

}
