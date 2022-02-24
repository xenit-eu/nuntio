package eu.xenit.nuntio.platform.docker.config.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InspectContainerMetadataTest {

    ContainerConfig containerConfig;
    NetworkSettings networkSettings;
    InspectContainerResponse inspectContainerResponse;
    InspectContainerMetadata inspectContainerMetadata;

    @BeforeEach
    void setup() {
        containerConfig = mock(ContainerConfig.class);
        networkSettings = mock(NetworkSettings.class);
        inspectContainerResponse = mock(InspectContainerResponse.class);

        when(inspectContainerResponse.getConfig()).thenReturn(containerConfig);
        when(inspectContainerResponse.getNetworkSettings()).thenReturn(networkSettings);

        inspectContainerMetadata = new InspectContainerMetadata(inspectContainerResponse);
    }

    @Test
    void simplePassthrough() {
        when(containerConfig.getImage()).thenReturn("my-fancy-image");
        when(containerConfig.getEnv()).thenReturn(new String[]{
                "SOME=value",
                "EMPTY=",
        });
        when(containerConfig.getLabels()).thenReturn(Map.of("label1", "value1"));

        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(80), Binding.bindIp("127.0.0.2"));
        ports.bind(ExposedPort.udp(53), Binding.bindPort(53));
        when(networkSettings.getPorts()).thenReturn(ports);


        assertEquals("my-fancy-image", inspectContainerMetadata.getImageName());
        assertEquals(Map.of("SOME", "value", "EMPTY", ""), inspectContainerMetadata.getEnvironment());
        assertEquals(Map.of("label1", "value1"), inspectContainerMetadata.getLabels());

        assertEquals(Set.of(
                ServiceBinding.fromPort(80),
                ServiceBinding.fromPortAndProtocol(53, "udp")
        ), inspectContainerMetadata.getInternalPortBindings());

    }

    @Test
    void specialEnvVars() {
        when(containerConfig.getEnv()).thenReturn(new String[]{
                "EMPTY=",
                "no_proxy", // Note: this envvar is inherited from the host, we just ignore it because we can't get the value in a sensible way
                "triple=equals=sign"
        });


        assertEquals(Map.of(
                "EMPTY", "",
                "triple", "equals=sign"
        ), inspectContainerMetadata.getEnvironment());
    }

    @Test
    void noPorts() {
        when(networkSettings.getPorts()).thenReturn(null);

        assertEquals(Collections.emptySet(), inspectContainerMetadata.getInternalPortBindings());
    }

}