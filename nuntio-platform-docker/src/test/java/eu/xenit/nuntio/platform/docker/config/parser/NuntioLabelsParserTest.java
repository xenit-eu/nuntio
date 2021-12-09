package eu.xenit.nuntio.platform.docker.config.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NuntioLabelsParserTest {

    private final NuntioLabelsParser labelsParser = new NuntioLabelsParser("nuntio");

    @Test
    void parseLabelsSimpleAny() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("nuntio/service", "value1")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);


        assertEquals(Collections.singleton(PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.ANY)
                .serviceName("value1")
                .build()),
                serviceConfigurations
        );
    }

    @Test
    void parseLabelsAdditionalAny() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("nuntio/service", "value1")
                .label("nuntio/metadata/some-metadata", "value2")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);


        assertEquals(Collections.singleton(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.ANY)
                        .serviceName("value1")
                        .serviceMetadata("some-metadata", "value2")
                        .build()),
                serviceConfigurations
        );
    }

    @Test
    void parseLabelsSimplePort() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("nuntio/80/service", "value1")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);


        assertEquals(Collections.singleton(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPort(80))
                        .serviceName("value1")
                        .build()),
                serviceConfigurations
        );
    }

    @Test
    void parseLabelsAdditionalPort() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("nuntio/80/service", "value1")
                .label("nuntio/80/metadata/some-metadata", "value2")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);


        assertEquals(Collections.singleton(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPort(80))
                        .serviceName("value1")
                        .serviceMetadata("some-metadata", "value2")
                        .build()),
                serviceConfigurations
        );
    }

    @Test
    void parseLabelsSimplePortProtocol() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("nuntio/udp:80/service", "value1")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);


        assertEquals(Collections.singleton(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPortAndProtocol(80, "udp"))
                        .serviceName("value1")
                        .build()),
                serviceConfigurations
        );
    }

    @Test
    void parseLabelsAdditionalPortProtocol() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("nuntio/udp:80/service", "value1")
                .label("nuntio/udp:80/metadata/some-metadata", "value2")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);

        assertEquals(Collections.singleton(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPortAndProtocol(80, "udp"))
                        .serviceName("value1")
                        .serviceMetadata("some-metadata", "value2")
                        .build()),
                serviceConfigurations
        );
    }

    @Test
    void parseLabelsUnknownKind() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("nuntio/8080/bla", "value1")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);

        assertEquals(Collections.emptySet(), serviceConfigurations);
    }

    @Test
    void parseLabelsOtherNamespace() {
        SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                .label("something-else/8080/bla", "value1")
                .build();
        Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(containerMetadata);

        assertEquals(Collections.emptySet(), serviceConfigurations);
    }

}
