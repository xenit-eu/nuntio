package be.vbgn.nuntio.platform.docker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.config.InternalNetworkConfigurationModifier.DockerNetworksFetcher;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InternalNetworkConfigurationModifierTest {

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    private static InspectContainerResponse responseWithNetworks(Map<String, ContainerNetwork> networks) {
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        Mockito.doReturn(networkSettings).when(inspectContainerResponse).getNetworkSettings();
        Mockito.doReturn(networks).when(networkSettings).getNetworks();
        return inspectContainerResponse;
    }

    private static void setContainerPid(InspectContainerResponse inspectContainerResponse, long pid) {
        ContainerState state = mock(ContainerState.class);
        Mockito.doReturn(state).when(inspectContainerResponse).getState();
        Mockito.doReturn(pid).when(state).getPidLong();
    }

    @Test
    void withoutNetworks() {
        DockerNetworksFetcher networksFetcher = filter -> Stream.of("n1id", "n2id");

        var networks = new HashMap<String, ContainerNetwork>();
        InspectContainerResponse inspectContainerResponse = responseWithNetworks(networks);
        setContainerPid(inspectContainerResponse, 1);

        var configurationModifier = new InternalNetworkConfigurationModifier(networksFetcher, "");

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), newConfiguration);
    }

    @Test
    void withMatchingBindingWithNetwork() {
        DockerNetworksFetcher networksFetcher = filter -> Stream.of("n1id", "n2id");

        var networks = new HashMap<String, ContainerNetwork>();
        ContainerNetwork network1 = mock(ContainerNetwork.class);
        networks.put("n1id", network1);
        InspectContainerResponse inspectContainerResponse = responseWithNetworks(networks);
        setContainerPid(inspectContainerResponse, 1);

        Mockito.doReturn("n1id").when(network1).getNetworkID();
        Mockito.doReturn("127.0.0.8").when(network1).getIpAddress();

        var configurationModifier = new InternalNetworkConfigurationModifier(networksFetcher, "");

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(8080, "tcp").withIp("127.0.0.8"))
        ), newConfiguration);
    }

    @Test
    void withMatchingBindingWithMultipleNetworks() {
        DockerNetworksFetcher networksFetcher = filter -> Stream.of("n1id", "n2id");

        var networks = new HashMap<String, ContainerNetwork>();
        ContainerNetwork network1 = mock(ContainerNetwork.class);
        ContainerNetwork network2 = mock(ContainerNetwork.class);
        networks.put("n1id", network1);
        networks.put("n2id", network2);
        InspectContainerResponse inspectContainerResponse = responseWithNetworks(networks);
        setContainerPid(inspectContainerResponse, 1);

        Mockito.doReturn("n1id").when(network1).getNetworkID();
        Mockito.doReturn("127.0.0.8").when(network1).getIpAddress();
        Mockito.doReturn("n2id").when(network2).getNetworkID();
        Mockito.doReturn("127.0.0.9").when(network2).getIpAddress();

        var configurationModifier = new InternalNetworkConfigurationModifier(networksFetcher, "");

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), newConfiguration);
    }

    @Test
    void withMatchingBindingWithMultipleNetworksOnlyOneMatching() {
        DockerNetworksFetcher networksFetcher = filter -> Stream.of("n1id", "n2id").filter(netId ->
                Optional.ofNullable(filter.getOrDefault("id", null))
                        .map(expectedId -> expectedId.contains(netId))
                        .orElse(true)
        );

        var networks = new HashMap<String, ContainerNetwork>();
        ContainerNetwork network1 = mock(ContainerNetwork.class);
        ContainerNetwork network2 = mock(ContainerNetwork.class);
        networks.put("n1id", network1);
        networks.put("n2id", network2);
        InspectContainerResponse inspectContainerResponse = responseWithNetworks(networks);
        setContainerPid(inspectContainerResponse, 1);

        Mockito.doReturn("n1id").when(network1).getNetworkID();
        Mockito.doReturn("127.0.0.8").when(network1).getIpAddress();
        Mockito.doReturn("n2id").when(network2).getNetworkID();
        Mockito.doReturn("127.0.0.9").when(network2).getIpAddress();

        var configurationModifier = new InternalNetworkConfigurationModifier(networksFetcher, "id=n1id");

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(8080, "tcp").withIp("127.0.0.8"))
        ), newConfiguration);
    }

    @Test
    void withStoppedContainer() {
        DockerNetworksFetcher networksFetcher = filter -> Stream.of("n1id", "n2id");

        var networks = new HashMap<String, ContainerNetwork>();
        ContainerNetwork network1 = mock(ContainerNetwork.class);
        networks.put("n1id", network1);
        InspectContainerResponse inspectContainerResponse = responseWithNetworks(networks);
        setContainerPid(inspectContainerResponse, 0);

        Mockito.doReturn("n1id").when(network1).getNetworkID();
        Mockito.doReturn("127.0.0.8").when(network1).getIpAddress();

        var configurationModifier = new InternalNetworkConfigurationModifier(networksFetcher, "");

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Collections.singleton(configuration), newConfiguration);
    }

    @Test
    void networkFiltersRunOnce() {
        DockerNetworksFetcher networksFetcher = new DockerNetworksFetcher() {
            @Override
            public Stream<String> getNetworkIdsMatching(Map<String, Set<String>> filter) {
                return Stream.of("n1id", "n2id");
            }
        };
        DockerNetworksFetcher networksFetcherSpy = Mockito.spy(networksFetcher);

        var networks = new HashMap<String, ContainerNetwork>();
        ContainerNetwork network1 = mock(ContainerNetwork.class);
        networks.put("n1id", network1);
        InspectContainerResponse inspectContainerResponse = responseWithNetworks(networks);
        setContainerPid(inspectContainerResponse, 1);

        Mockito.doReturn("n1id").when(network1).getNetworkID();
        Mockito.doReturn("127.0.0.8").when(network1).getIpAddress();

        var configurationModifier = new InternalNetworkConfigurationModifier(networksFetcherSpy,
                "id=n1id,id=n2id,a=b,c=b");

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        configurationModifier.modifyConfiguration(configuration, inspectContainerResponse);
        configurationModifier.modifyConfiguration(configuration, inspectContainerResponse);

        Mockito.verify(networksFetcherSpy)
                .getNetworkIdsMatching(Map.of("id", Set.of("n1id", "n2id"), "a", Set.of("b"), "c", Set.of("b")));

    }

    @Test
    void networkFiltersFetchAgainOnUnknownNetwork() {
        DockerNetworksFetcher networksFetcher = new DockerNetworksFetcher() {
            @Override
            public Stream<String> getNetworkIdsMatching(Map<String, Set<String>> filter) {
                return Stream.of("n1id", "n2id");
            }
        };
        DockerNetworksFetcher networksFetcherSpy = Mockito.spy(networksFetcher);

        var networks = new HashMap<String, ContainerNetwork>();
        ContainerNetwork network1 = mock(ContainerNetwork.class);
        networks.put("n1id", network1);
        InspectContainerResponse inspectContainerResponse = responseWithNetworks(networks);
        setContainerPid(inspectContainerResponse, 1);

        Mockito.doReturn("n1id").when(network1).getNetworkID();
        Mockito.doReturn("127.0.0.8").when(network1).getIpAddress();

        var configurationModifier = new InternalNetworkConfigurationModifier(networksFetcherSpy,
                "id=n1id,id=n2id,a=b,c=b");

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        configurationModifier.modifyConfiguration(configuration, inspectContainerResponse);
        Mockito.verify(networksFetcherSpy)
                .getNetworkIdsMatching(Map.of("id", Set.of("n1id", "n2id"), "a", Set.of("b"), "c", Set.of("b")));

        Mockito.doReturn("n3id").when(network1).getNetworkID();
        configurationModifier.modifyConfiguration(configuration, inspectContainerResponse);

        Mockito.verify(networksFetcherSpy, Mockito.times(2))
                .getNetworkIdsMatching(Map.of("id", Set.of("n1id", "n2id"), "a", Set.of("b"), "c", Set.of("b")));
    }
}
