package eu.xenit.nuntio.platform.docker.config.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import eu.xenit.nuntio.registry.fake.checks.FakeCheck;
import eu.xenit.nuntio.registry.fake.checks.FakeCheckFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NuntioLabelsParserTest {

    private final NuntioLabelsParser labelsParser = new NuntioLabelsParser("nuntio", new FakeCheckFactory());

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

    @Nested
    class CheckLabels {
        @Test
        void oneCheck() {
            SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                    .label("nuntio/service", "value1")
                    .label("nuntio/check/xyz/type", "fake:http")
                    .label("nuntio/check/xyz/method", "plan")
                    .label("nuntio/check/xyz/tls", "off")
                    .build();

            Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(
                    containerMetadata);

            assertEquals(Collections.singleton(PlatformServiceConfiguration.builder()
                            .serviceBinding(ServiceBinding.ANY)
                            .serviceName("value1")
                            .check(new FakeCheck("xyz", "fake:http", Map.of("method", "plan", "tls", "off")))
                            .build()),
                    serviceConfigurations
            );

        }

        @Test
        void noInheritedChecks() {
            SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                    .label("nuntio/service", "value1")
                    .label("nuntio/check/xyz/type", "fake:http")
                    .label("nuntio/check/xyz/method", "GET")
                    .label("nuntio/80/service", "value2")
                    .label("nuntio/80/check/xyz/tls", "off")
                    .label("nuntio/443/service", "value3")
                    .label("nuntio/443/check/xyz/type", "fake:http")
                    .label("nuntio/443/check/xyz/tls", "on")
                    .build();

            Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(
                    containerMetadata);

            assertEquals(Set.of(
                            PlatformServiceConfiguration.builder()
                                    .serviceBinding(ServiceBinding.ANY)
                                    .serviceName("value1")
                                    .check(new FakeCheck("xyz", "fake:http", Map.of("method", "GET")))
                                    .build(),
                            PlatformServiceConfiguration.builder()
                                    .serviceBinding(ServiceBinding.fromPort(80))
                                    .serviceName("value2")
                                    .build(),
                            PlatformServiceConfiguration.builder()
                                    .serviceBinding(ServiceBinding.fromPort(443))
                                    .serviceName("value3")
                                    .check(new FakeCheck("xyz", "fake:http", Map.of( "tls", "on")))
                                    .build()
                    ),
                    serviceConfigurations
            );
        }

        @Test
        void sameNameOnDifferentPortChecks() {
            SimpleContainerMetadata containerMetadata = SimpleContainerMetadata.builder()
                    .label("nuntio/service", "value1")
                    .label("nuntio/check/xyz/method", "GET")
                    .label("nuntio/80/check/xyz/type", "fake:http")
                    .label("nuntio/80/service", "value1")
                    .label("nuntio/443/check/xyz/type", "fake:http")
                    .label("nuntio/443/service", "value1")
                    .build();

            Set<PlatformServiceConfiguration> serviceConfigurations = labelsParser.toServiceConfigurations(
                    containerMetadata);

            assertEquals(Set.of(
                            PlatformServiceConfiguration.builder()
                                    .serviceName("value1")
                                    .serviceBinding(ServiceBinding.ANY)
                                    .build(),
                            PlatformServiceConfiguration.builder()
                                    .serviceBinding(ServiceBinding.fromPort(80))
                                    .serviceName("value1")
                                    .check(new FakeCheck("xyz", "fake:http", Collections.emptyMap()))
                                    .build(),
                            PlatformServiceConfiguration.builder()
                                    .serviceBinding(ServiceBinding.fromPort(443))
                                    .serviceName("value1")
                                    .check(new FakeCheck("xyz", "fake:http", Collections.emptyMap()))
                                    .build()
                    ),
                    serviceConfigurations
            );
        }
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
