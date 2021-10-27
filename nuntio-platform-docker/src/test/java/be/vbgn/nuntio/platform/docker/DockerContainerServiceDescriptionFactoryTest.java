package be.vbgn.nuntio.platform.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.config.modifier.ServiceConfigurationModifier;
import be.vbgn.nuntio.platform.docker.config.parser.NuntioLabelsParser;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DockerContainerServiceDescriptionFactoryTest {

    private DockerContainerServiceDescriptionFactory dockerContainerServiceDescriptionFactory;
    private ServiceConfigurationModifier mockServiceConfigurationModifier;

    @BeforeEach
    void setup() {
        mockServiceConfigurationModifier = mock(ServiceConfigurationModifier.class);

        dockerContainerServiceDescriptionFactory = new DockerContainerServiceDescriptionFactory(
                new NuntioLabelsParser("nuntio"),
                Collections.singletonList(mockServiceConfigurationModifier)
        );
    }

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    private static InspectContainerResponse mockInspectContainerWithLabels(Map<String, String> labels) {
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        ContainerConfig containerConfig = mock(ContainerConfig.class);
        Mockito.doReturn(containerConfig).when(inspectContainerResponse).getConfig();
        Mockito.doReturn(labels).when(containerConfig).getLabels();
        return inspectContainerResponse;
    }

    private void doNotModifyServiceConfiguration(InspectContainerResponse inspectContainerResponse) {
        Mockito.doAnswer(invocation -> Stream.of(invocation.getArgument(0, PlatformServiceConfiguration.class)))
                .when(mockServiceConfigurationModifier)
                .modifyConfiguration(Mockito.any(), Mockito.eq(inspectContainerResponse));
    }

    @Test
    void createServiceConfigurations() {
        Map<String, String> labels = new HashMap<>();
        labels.put("nuntio/service", "some-service");
        labels.put("nuntio/tcp:8500/service", "consul-api");
        labels.put("nuntio/tcp:8500/tags", "tcp,api");
        labels.put("nuntio/tcp:8500/metadata/published-domain", "consul.internal");

        InspectContainerResponse inspectContainerResponse = mockInspectContainerWithLabels(labels);
        doNotModifyServiceConfiguration(inspectContainerResponse);

        PlatformServiceDescription serviceDescription = dockerContainerServiceDescriptionFactory.createServiceDescription(
                inspectContainerResponse);

        Set<PlatformServiceConfiguration> configurations = serviceDescription.getServiceConfigurations();

        Mockito.verify(mockServiceConfigurationModifier, times(2))
                .modifyConfiguration(Mockito.any(), Mockito.eq(inspectContainerResponse));

        assertEquals(Set.of(
                PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.ANY)
                        .serviceName("some-service")
                        .build(),
                PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPortAndProtocol(8500, "tcp"))
                        .serviceName("consul-api")
                        .serviceTag("tcp")
                        .serviceTag("api")
                        .serviceMetadata("published-domain", "consul.internal")
                        .build()
        ), configurations);

    }

    @Test
    void createServiceConfigurationsEmptyService() {
        Map<String, String> labels = new HashMap<>();
        labels.put("nuntio/service", "");
        labels.put("nuntio/udp:8600/service", "");

        InspectContainerResponse inspectContainerResponse = mockInspectContainerWithLabels(labels);
        doNotModifyServiceConfiguration(inspectContainerResponse);

        PlatformServiceDescription serviceDescription = dockerContainerServiceDescriptionFactory.createServiceDescription(
                inspectContainerResponse);

        Set<PlatformServiceConfiguration> configurations = serviceDescription.getServiceConfigurations();

        assertEquals(Collections.emptySet(), configurations);
    }

    @Test
    void createServiceConfigurationsOnlyTags() {
        Map<String, String> labels = new HashMap<>();
        labels.put("nuntio/tags", "some-tag");

        InspectContainerResponse inspectContainerResponse = mockInspectContainerWithLabels(labels);
        doNotModifyServiceConfiguration(inspectContainerResponse);

        PlatformServiceDescription serviceDescription = dockerContainerServiceDescriptionFactory.createServiceDescription(
                inspectContainerResponse);

        Set<PlatformServiceConfiguration> configurations = serviceDescription.getServiceConfigurations();

        assertEquals(Collections.emptySet(), configurations);
    }

    @Test
    void createServiceConfigurationsOnlyMetadata() {
        Map<String, String> labels = new HashMap<>();
        labels.put("nuntio/metadata/xyz", "bla");

        InspectContainerResponse inspectContainerResponse = mockInspectContainerWithLabels(labels);
        doNotModifyServiceConfiguration(inspectContainerResponse);

        PlatformServiceDescription serviceDescription = dockerContainerServiceDescriptionFactory.createServiceDescription(
                inspectContainerResponse);

        Set<PlatformServiceConfiguration> configurations = serviceDescription.getServiceConfigurations();

        assertEquals(Collections.emptySet(), configurations);
    }
}
