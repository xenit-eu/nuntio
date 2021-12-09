package eu.xenit.nuntio.platform.docker.config.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import eu.xenit.nuntio.platform.docker.DockerProperties.RegistratorCompatibleProperties;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RegistratorCompatibleParserTest {


    private RegistratorCompatibleProperties registratorCompatibleProperties;
    private ServiceConfigurationParser configurationParser;

    @BeforeEach
    void setUp() {
        registratorCompatibleProperties = new RegistratorCompatibleProperties();
        configurationParser = new RegistratorCompatibleParser(registratorCompatibleProperties);
    }

    @Test
    void singleServiceWithDefaults() {
        // docker run -d --name redis.0 -p 10000:6379 progrium/redis
        var desc = SimpleContainerMetadata.builder()
                .imageName("progrium/redis")
                .internalPortBinding(ServiceBinding.fromPort(6379))
                .build();

        var service = configurationParser.toServiceConfigurations(desc);

        assertEquals(
                Collections.singleton(
                        PlatformServiceConfiguration.builder()
                                .serviceName("redis")
                                .serviceBinding(ServiceBinding.fromPort(6379))
                                .build()
                ),
                service
        );

    }

    @Test
    void singleServiceWithDefaults_defaultIgnore() {
        registratorCompatibleProperties.setExplicit(true);

        // docker run -d --name redis.0 -p 10000:6379 progrium/redis
        var desc = SimpleContainerMetadata.builder()
                .imageName("progrium/redis")
                .internalPortBinding(ServiceBinding.fromPort(6379))
                .build();

        var service = configurationParser.toServiceConfigurations(desc);

        assertEquals(Collections.emptySet(), service);
    }

    @Test
    void singleService_withEnvMetadata() {
        // $ docker run -d --name redis.0 -p 10000:6379 -e "SERVICE_NAME=db" progrium/redis
        var desc = SimpleContainerMetadata.builder()
                .imageName("progrium/redis")
                .environment("SERVICE_NAME", "db")
                .environment("SERVICE_TAGS", "master,backups")
                .environment("SERVICE_REGION", "us2")
                .internalPortBinding(ServiceBinding.fromPort(6379))
                .build();

        var service = configurationParser.toServiceConfigurations(desc);

        assertEquals(
                Collections.singleton(
                        PlatformServiceConfiguration.builder()
                                .serviceName("db")
                                .serviceBinding(ServiceBinding.fromPort(6379))
                                .serviceTag("master")
                                .serviceTag("backups")
                                .serviceMetadata("region", "us2")
                                .build()
                ),
                service
        );
    }

    @Test
    void multipleServices_withDefaults() {
        // $ docker run -d --name nginx.0 -p 4443:443 -p 8000:80 progrium/nginx
        var desc = SimpleContainerMetadata.builder()
                .imageName("progrium/redis")
                .internalPortBinding(ServiceBinding.fromPort(443))
                .internalPortBinding(ServiceBinding.fromPort(80))
                .build();
        var service = configurationParser.toServiceConfigurations(desc);

        assertEquals(
                Set.of(
                        PlatformServiceConfiguration.builder()
                                .serviceName("redis-443")
                                .serviceBinding(ServiceBinding.fromPort(443))
                                .build(),

                        PlatformServiceConfiguration.builder()
                                .serviceName("redis-80")
                                .serviceBinding(ServiceBinding.fromPort(80))
                                .build()
                ),
                service
        );
    }

    @Test
    void multipleServices_withMetadata() {
        // $ docker run -d --name nginx.0 -p 4443:443 -p 8000:80 \
        //            -e "SERVICE_443_NAME=https" \
        //            -e "SERVICE_443_ID=https.12345" \
        //            -e "SERVICE_443_SNI=enabled" \
        //            -e "SERVICE_80_NAME=http" \
        //            -e "SERVICE_TAGS=www" progrium/nginx

        var desc = SimpleContainerMetadata.builder()
                .imageName("progrium/redis")
                .internalPortBinding(ServiceBinding.fromPort(443))
                .internalPortBinding(ServiceBinding.fromPort(80))
                .environment("SERVICE_443_NAME", "https")
                .environment("SERVICE_443_ID", "https.12345")
                .environment("SERVICE_443_SNI", "enabled")
                .environment("SERVICE_80_NAME", "http")
                .environment("SERVICE_80_TAGS", "www")
                .build();

        var service = configurationParser.toServiceConfigurations(desc);

        assertEquals(
                Set.of(
                        PlatformServiceConfiguration.builder()
                                .serviceName("https")
                                .serviceBinding(ServiceBinding.fromPort(443))
                                .serviceMetadata("sni", "enabled")
                                .build(),

                        PlatformServiceConfiguration.builder()
                                .serviceName("http")
                                .serviceBinding(ServiceBinding.fromPort(80))
                                .serviceTag("www")
                                .build()
                ),
                service
        );
    }

    @Test
    void multipleServices_withMetadata_defaultIgnore() {
        registratorCompatibleProperties.setExplicit(true);

        // $ docker run -d --name nginx.0 -p 4443:443 -p 8000:80 \
        //            -e "SERVICE_443_NAME=https" \
        //            -e "SERVICE_443_ID=https.12345" \
        //            -e "SERVICE_443_SNI=enabled" \
        //            -e "SERVICE_80_NAME=http" \
        //            -e "SERVICE_TAGS=www" progrium/nginx

        var desc = SimpleContainerMetadata.builder()
                .imageName("progrium/redis")
                .internalPortBinding(ServiceBinding.fromPort(443))
                .internalPortBinding(ServiceBinding.fromPort(80))
                .environment("SERVICE_443_NAME", "https")
                .environment("SERVICE_443_ID", "https.12345")
                .environment("SERVICE_443_SNI", "enabled")
                .environment("SERVICE_80_NAME", "http")
                .environment("SERVICE_80_TAGS", "www")
                .build();

        var service = configurationParser.toServiceConfigurations(desc);

        assertEquals(
                Set.of(
                        PlatformServiceConfiguration.builder()
                                .serviceName("https")
                                .serviceBinding(ServiceBinding.fromPort(443))
                                .serviceMetadata("sni", "enabled")
                                .build(),

                        PlatformServiceConfiguration.builder()
                                .serviceName("http")
                                .serviceBinding(ServiceBinding.fromPort(80))
                                .serviceTag("www")
                                .build()
                ),
                service
        );
    }

    @Nested
    class TestServiceName {

        @Test
        public void explicitServiceName() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8000))
                    .environment("SERVICE_NAME", "redis")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis")
                            .serviceBinding(ServiceBinding.fromPort(8000))
                            .build()
            ), services);
        }

        @Test
        public void defaultServiceName() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8000))
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis")
                            .serviceBinding(ServiceBinding.fromPort(8000))
                            .build()
            ), services);
        }

        @Test
        public void withLabel() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis:1.2.3")
                    .internalPortBinding(ServiceBinding.fromPort(8000))
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis")
                            .serviceBinding(ServiceBinding.fromPort(8000))
                            .build()
            ), services);
        }

        @Test
        public void withoutNamespace() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("redis:1.2.3")
                    .internalPortBinding(ServiceBinding.fromPort(8000))
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis")
                            .serviceBinding(ServiceBinding.fromPort(8000))
                            .build()
            ), services);
        }

        @Test
        public void withHostname() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("docker.io/progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8000))
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis")
                            .serviceBinding(ServiceBinding.fromPort(8000))
                            .build()
            ), services);
        }
    }

    @Nested
    class TestMeta {

        @Test
        void withDefault() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8080))
                    .environment("SERVICE_FOO", "foo")
                    .environment("SERVICE_BAR", "bar")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis")
                            .serviceBinding(ServiceBinding.fromPort(8080))
                            .serviceMetadata("foo", "foo")
                            .serviceMetadata("bar", "bar")
                            .build()
            ), services);
        }

        @Test
        void withPort() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8080))
                    .environment("SERVICE_FOO", "fail")
                    .environment("SERVICE_BAR", "fail")
                    .environment("SERVICE_8080_FOO", "ok")
                    .environment("SERVICE_8080_BAR", "ok")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis")
                            .serviceBinding(ServiceBinding.fromPort(8080))
                            .serviceMetadata("foo", "ok")
                            .serviceMetadata("bar", "ok")
                            .build()
            ), services);
        }

        @Test
        void blankName() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8080))
                    .internalPortBinding(ServiceBinding.fromPort(8081))
                    .environment("SERVICE_", "empty")
                    .environment("SERVICE_8080_", "empty")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);

            assertEquals(Set.of(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis-8080")
                            .serviceBinding(ServiceBinding.fromPort(8080))
                            .serviceMetadata("", "empty")
                            .build(),

                    PlatformServiceConfiguration.builder()
                            .serviceName("redis-8081")
                            .serviceBinding(ServiceBinding.fromPort(8081))
                            .serviceMetadata("", "empty")
                            .build()
            ), services);
        }

    }

    @Nested
    class TestIgnore {
        @Test
        void globalIgnore() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8080))
                    .internalPortBinding(ServiceBinding.fromPort(8081))
                    .environment("SERVICE_IGNORE", "true")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);
            assertEquals(Collections.emptySet(), services);
        }

        @Test
        void globalIgnoreEmpty() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8080))
                    .internalPortBinding(ServiceBinding.fromPort(8081))
                    .environment("SERVICE_IGNORE", "")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);
            assertEquals(Set.of(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis-8080")
                            .serviceBinding(ServiceBinding.fromPort(8080))
                            .serviceMetadata("ignore", "")
                            .build(),
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis-8081")
                            .serviceBinding(ServiceBinding.fromPort(8081))
                            .serviceMetadata("ignore", "")
                            .build()
                    ), services);
        }

        @Test
        void servicePortIgnore() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8080))
                    .internalPortBinding(ServiceBinding.fromPort(8081))
                    .environment("SERVICE_8080_IGNORE", "1")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);
            assertEquals(Collections.singleton(
                    PlatformServiceConfiguration.builder()
                            .serviceName("redis-8081")
                            .serviceBinding(ServiceBinding.fromPort(8081))
                            .build()

            ), services);
        }

        @Test
        void globalIgnoreWithServiceName() {
            var redis = SimpleContainerMetadata.builder()
                    .imageName("progrium/redis")
                    .internalPortBinding(ServiceBinding.fromPort(8080))
                    .internalPortBinding(ServiceBinding.fromPort(8081))
                    .environment("SERVICE_IGNORE", "1")
                    .environment("SERVICE_8081_NAME", "xyz")
                    .build();

            var services = configurationParser.toServiceConfigurations(redis);
            assertEquals(Collections.emptySet(), services);
        }
    }
}
