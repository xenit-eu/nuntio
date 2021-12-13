package eu.xenit.nuntio.platform.docker.config.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.api.command.InspectContainerResponse;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import eu.xenit.nuntio.platform.docker.DockerProperties.AddressFamilies;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class RemoveDisabledBindFamiliesAddressConfigurationModifierTest {

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    @Value
    private static class IpAddr {
        String addr;
        boolean isIpv6;
    }

    private static Stream<IpAddr> addresses() {
        return Stream.of(
                new IpAddr("0.0.0.0", false),
                new IpAddr("127.0.0.1", false),
                new IpAddr("192.168.85.69", false),
                new IpAddr("::", true),
                new IpAddr("::1", true),
                new IpAddr("fe80::5:8", true)

        );
    }

    @ParameterizedTest
    @MethodSource("addresses")
    void removeNoBinds(IpAddr ip) {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080).withIp(ip.getAddr()))
                .build();

        var modifier = new RemoveDisabledBindFamiliesAddressConfigurationModifier(new AddressFamilies());

        var newConfigurations = modifier.modifyConfiguration(configuration, mock(InspectContainerResponse.class)).collect(
                Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.withBinding(ServiceBinding.fromPortAndProtocol(8080, "tcp").withIp(ip.getAddr()))
        ), newConfigurations);
    }

    @ParameterizedTest
    @MethodSource("addresses")
    void removeIpv6Binds(IpAddr ip) {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080).withIp(ip.getAddr()))
                .build();

        var addressFamilies = new AddressFamilies();
        addressFamilies.setIpv6(false);

        var modifier = new RemoveDisabledBindFamiliesAddressConfigurationModifier(addressFamilies);

        var newConfigurations = modifier.modifyConfiguration(configuration, mock(InspectContainerResponse.class)).collect(
                Collectors.toSet());

        if(!ip.isIpv6()) {
            assertEquals(Collections.singleton(
                    configuration.withBinding(ServiceBinding.fromPortAndProtocol(8080, "tcp").withIp(ip.getAddr()))
            ), newConfigurations);
        } else {
            assertEquals(Collections.emptySet(), newConfigurations);
        }
    }

    @ParameterizedTest
    @MethodSource("addresses")
    void removeIpv4Binds(IpAddr ip) {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080).withIp(ip.getAddr()))
                .build();

        var addressFamilies = new AddressFamilies();
        addressFamilies.setIpv4(false);

        var modifier = new RemoveDisabledBindFamiliesAddressConfigurationModifier(addressFamilies);

        var newConfigurations = modifier.modifyConfiguration(configuration, mock(InspectContainerResponse.class)).collect(
                Collectors.toSet());

        if(ip.isIpv6()) {
            assertEquals(Collections.singleton(
                    configuration.withBinding(ServiceBinding.fromPortAndProtocol(8080, "tcp").withIp(ip.getAddr()))
            ), newConfigurations);
        } else {
            assertEquals(Collections.emptySet(), newConfigurations);
        }
    }

    @ParameterizedTest
    @MethodSource("addresses")
    void removeAllBinds(IpAddr ip) {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080).withIp(ip.getAddr()))
                .build();

        var addressFamilies = new AddressFamilies();
        addressFamilies.setIpv4(false);
        addressFamilies.setIpv6(false);

        var modifier = new RemoveDisabledBindFamiliesAddressConfigurationModifier(addressFamilies);

        var newConfigurations = modifier.modifyConfiguration(configuration, mock(InspectContainerResponse.class)).collect(
                Collectors.toSet());

        assertEquals(Collections.emptySet(), newConfigurations);
    }

}