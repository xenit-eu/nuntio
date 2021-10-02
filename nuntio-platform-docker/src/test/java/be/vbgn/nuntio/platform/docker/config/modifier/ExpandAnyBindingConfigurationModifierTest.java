package be.vbgn.nuntio.platform.docker.config.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.config.modifier.ExpandAnyBindingConfigurationModifier;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExpandAnyBindingConfigurationModifierTest {

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    private static InspectContainerResponse responseWithExposedPorts(ExposedPort[] exposedPort) {
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        ContainerConfig containerConfig = mock(ContainerConfig.class);
        Mockito.doReturn(containerConfig).when(inspectContainerResponse).getConfig();
        Mockito.doReturn(exposedPort).when(containerConfig).getExposedPorts();
        return inspectContainerResponse;
    }

    @Test
    void expandAnyBindingNoExposedPorts() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceName("some-service")
                .serviceBinding(ServiceBinding.ANY)
                .build();

        var configurationModifier = new ExpandAnyBindingConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration, responseWithExposedPorts(null))
                .collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), newConfiguration);
    }

    @Test
    void expandAnyBindingOneExposedPort() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceName("some-service")
                .serviceBinding(ServiceBinding.ANY)
                .build();

        var configurationModifier = new ExpandAnyBindingConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration,
                        responseWithExposedPorts(new ExposedPort[]{
                                new ExposedPort(553),
                        }))
                .collect(Collectors.toSet());

        assertEquals(Collections.singleton(configuration.toBuilder()
                .serviceBinding(ServiceBinding.fromPortAndProtocol(553, "tcp"))
                .build()), newConfiguration);
    }

    @Test
    void expandAnyBindingManyExposedPorts() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceName("some-service")
                .serviceBinding(ServiceBinding.ANY)
                .build();

        var configurationModifier = new ExpandAnyBindingConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration,
                        responseWithExposedPorts(new ExposedPort[]{
                                new ExposedPort(553),
                                new ExposedPort(134, InternetProtocol.UDP)
                        }))
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                configuration.toBuilder().serviceBinding(ServiceBinding.fromPortAndProtocol(553, "tcp")).build(),
                configuration.toBuilder().serviceBinding(ServiceBinding.fromPortAndProtocol(134, "udp")).build()
        ), newConfiguration);
    }

    @Test
    void doNotExpandOtherBinding() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceName("some-service")
                .serviceBinding(ServiceBinding.fromPortAndProtocol(553, "tcp"))
                .build();

        var configurationModifier = new ExpandAnyBindingConfigurationModifier();

        var newConfiguration = configurationModifier.modifyConfiguration(configuration,
                        responseWithExposedPorts(new ExposedPort[]{
                                new ExposedPort(553),
                                new ExposedPort(134, InternetProtocol.UDP)
                        }))
                .collect(Collectors.toSet());

        assertEquals(Set.of(configuration), newConfiguration);
    }

}
