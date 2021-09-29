package be.vbgn.nuntio.platform.docker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PublishedPortConfigurationModifierTest {

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    private static InspectContainerResponse responseWithBindings(Ports ports) {
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        Mockito.doReturn(networkSettings).when(inspectContainerResponse).getNetworkSettings();
        Mockito.doReturn(ports).when(networkSettings).getPorts();
        return inspectContainerResponse;
    }

    private static void setContainerPid(InspectContainerResponse inspectContainerResponse, long pid) {
        ContainerState state = mock(ContainerState.class);
        Mockito.doReturn(state).when(inspectContainerResponse).getState();
        Mockito.doReturn(pid).when(state).getPidLong();
    }

    @Test
    void withoutBindings() {
        InspectContainerResponse inspectContainerResponse = responseWithBindings(new Ports());
        setContainerPid(inspectContainerResponse, 1);

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var configurationModifier = new PublishedPortConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .collect(
                        Collectors.toSet());

        assertEquals(Collections.emptySet(), newConfiguration);
    }

    @Test
    void withMatchingBindingWithoutIp() {
        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("0.0.0.0", 1234));
        InspectContainerResponse inspectContainerResponse = responseWithBindings(ports);
        setContainerPid(inspectContainerResponse, 1);

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var configurationModifier = new PublishedPortConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(1234, "tcp"))
        ), newConfiguration);
    }


    @Test
    void withMatchingBindingWithIp() {
        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("127.0.0.5", 1234));
        InspectContainerResponse inspectContainerResponse = responseWithBindings(ports);
        setContainerPid(inspectContainerResponse, 1);

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var configurationModifier = new PublishedPortConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(1234, "tcp").withIp("127.0.0.5"))
        ), newConfiguration);
    }

    @Test
    void withMatchingManyBindingWithoutIp() {
        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("0.0.0.0", 1234));
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("::", 1234));
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("::", 567));
        InspectContainerResponse inspectContainerResponse = responseWithBindings(ports);
        setContainerPid(inspectContainerResponse, 1);

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var configurationModifier = new PublishedPortConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(1234, "tcp")),
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(567, "tcp"))
        ), newConfiguration);
    }

    @Test
    void withMatchingManyBindingWithIP() {
        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("127.0.0.5", 1234));
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("::1", 1234));
        ports.bind(ExposedPort.tcp(8080), Binding.bindIpAndPort("127.0.0.8", 567));
        InspectContainerResponse inspectContainerResponse = responseWithBindings(ports);
        setContainerPid(inspectContainerResponse, 1);

        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var configurationModifier = new PublishedPortConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, inspectContainerResponse)
                .map(config -> config.toBuilder().clearInternalMetadata().build())
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(1234, "tcp").withIp("127.0.0.5")),
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(1234, "tcp").withIp("::1")),
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(567, "tcp").withIp("127.0.0.8"))
        ), newConfiguration);
    }
}
