package eu.xenit.nuntio.platform.docker.config.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.api.command.InspectContainerResponse;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RemoveAnyLocalAddressConfigurationModifierTest {
    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    @Test
    void removeIpv4AnyBind() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080).withIp("0.0.0.0"))
                .build();
        var modifier = new RemoveAnyLocalAddressConfigurationModifier();

        var newConfigurations = modifier.modifyConfiguration(configuration, mock(InspectContainerResponse.class)).collect(
                Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(8080, "tcp"))
        ), newConfigurations);
    }

    @Test
    void removeIpv6AnyBind() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080).withIp("::"))
                .build();
        var modifier = new RemoveAnyLocalAddressConfigurationModifier();

        var newConfigurations = modifier.modifyConfiguration(configuration, mock(InspectContainerResponse.class)).collect(
                Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(8080, "tcp"))
        ), newConfigurations);
    }

}