package eu.xenit.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assume.assumeThat;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import eu.xenit.nuntio.integtest.containers.NuntioContainer;
import eu.xenit.nuntio.integtest.containers.RegistrationContainer;
import eu.xenit.nuntio.integtest.containers.RegistratorContainer;
import eu.xenit.nuntio.integtest.jupiter.annotations.CompatTest;
import eu.xenit.nuntio.integtest.jupiter.annotations.ContainerTests;
import eu.xenit.nuntio.integtest.jupiter.annotations.NuntioTest;
import eu.xenit.nuntio.integtest.util.SimpleContainerInspect;
import eu.xenit.nuntio.integtest.util.SimpleContainerModifier;
import java.util.Collections;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContainerTests
public class ContainerRegistrationTest extends ContainerBaseTest {

    private NuntioContainer nuntioNormalContainer = new NuntioContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withRegistratorCompat(true);

    private NuntioContainer nuntioExplicitContainer = new NuntioContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withRegistratorCompat(true)
            .withRegistratorExplicitOnly(true);

    private NuntioContainer nuntioInternalContainer = new NuntioContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withRegistratorCompat(true)
            .withInternalPorts(true);

    private RegistratorContainer registratorNormalContainer = new RegistratorContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer);

    private RegistratorContainer registratorExplicitContainer = new RegistratorContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withRegistratorExplicitOnly(true);

    private RegistratorContainer registratorInternalContainer = new RegistratorContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withInternalPorts(true);

    @CompatTest
    void defaultConfig(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer container) {
        CreateContainerResponse myOtherServiceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        // Note: lowercase, so it is NOT picked up by registrator as a valid service
                        .andThen(SimpleContainerModifier.withLabel("service_name", "myotherservice"))
        );

        CreateContainerResponse myServiceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myservice"))
        );


        dockerClient.startContainerCmd(myOtherServiceContainer.getId()).exec();
        dockerClient.startContainerCmd(myServiceContainer.getId()).exec();

        var inspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(myServiceContainer.getId()).exec());
        var mappedPort = inspect.findSingleContainerBinding(ExposedPort.tcp(123)).getHostPortSpec();
        Matcher<String> serviceAddressMatcher = equalTo("127.0.0.1");

        if(container.isInternalPorts()) {
            serviceAddressMatcher = equalTo(inspect.findInternalIps().get("bridge"));
            mappedPort = "123";
        }

        await.until(consulWaiter().serviceExists("myservice"));

        var myserviceInstances = consulClient.getCatalogService("myservice", CatalogServiceRequest.newBuilder().build())
                .getValue();

        assertThat(myserviceInstances, hasSize(1));

        var service = myserviceInstances.get(0);
        assertThat(service.getAddress(), equalTo("127.0.0.1"));
        assertThat(service.getServiceAddress(), serviceAddressMatcher);
        assertThat(Integer.toString(service.getServicePort()), equalTo(mappedPort));

        var catalogServices = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();
        if(container.isRegistratorExplicitOnly()) {
            assertThat(catalogServices, not(hasKey("alpine")));
        } else {
            assertThat(catalogServices, hasKey("alpine"));
        }
    }

    @CompatTest
    void ipv6Mapping(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer container) {
        assumeThat("Ipv6 mapping only works for exposed ports", container.isInternalPorts(), equalTo(false));
        CreateContainerResponse myServiceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIpAndPort("0.0.0.0", 1123))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIpAndPort("::", 2123)))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(124), Binding.bindIpAndPort("::", 2124)))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(125), Binding.bindIpAndPort("0.0.0.0", 1125)))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_123_NAME", "myservice"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_124_NAME", "myservice"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_125_NAME", "myservice"))
        );


        dockerClient.startContainerCmd(myServiceContainer.getId()).exec();

        waitForFullCycle();

        await.until(consulWaiter().serviceExists("myservice"));

        var myserviceInstances = consulClient.getCatalogService("myservice", CatalogServiceRequest.newBuilder().build())
                .getValue();

        var servicePorts = myserviceInstances.stream().map(CatalogService::getServicePort).collect(Collectors.toList());
        if(container instanceof RegistratorContainer) {
            // Which one of the 2 binds for port 123 is mapped is pure luck with registrator
            assertThat(servicePorts, containsInAnyOrder(equalTo(2124), equalTo(1125), oneOf(1123, 2123)));
        } else {
            // In nuntio, all ports are properly mapped
            assertThat(servicePorts, containsInAnyOrder(equalTo(2124), equalTo(1125), equalTo(1123), equalTo(2123)));
        }
    }

    @CompatTest
    void multiplePorts(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse createContainer = createContainer(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindPort(666))
                .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(444), Binding.empty()))
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_NAME", "mont-blanc"))
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_444_TAGS", "tls"))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("mont-blanc-123"));
        await.until(consulWaiter().serviceExists("mont-blanc-444"));

        var services = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();

        assertThat(services, hasKey("mont-blanc-123"));
        assertThat(services, hasKey("mont-blanc-444"));
        assertThat(services, not(hasKey("mont-blanc")));

        var nottaggedService = consulClient.getCatalogService("mont-blanc-123", CatalogServiceRequest.newBuilder().build()).getValue().get(0);
        var taggedService = consulClient.getCatalogService("mont-blanc-444", CatalogServiceRequest.newBuilder().build()).getValue().get(0);

        assertThat(nottaggedService.getServiceTags(), emptyCollectionOf(String.class));
        assertThat(taggedService.getServiceTags(), equalTo(Collections.singletonList("tls")));
    }

    @CompatTest
    void metadata(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse createContainer = createContainer(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.empty())
                .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(444), Binding.empty()))
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_NAME", "mont-blanc"))
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_444_NAME", "mont-gris"))
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_REGION", "us-east"))
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_444_REGION", "us-west"))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("mont-blanc-123"));
        await.until(consulWaiter().serviceExists("mont-gris"));

        var services = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();

        assertThat(services, hasKey("mont-blanc-123"));
        assertThat(services, not(hasKey("mont-blanc")));
        assertThat(services, hasKey("mont-gris"));

        var montBlancService = consulClient.getCatalogService("mont-blanc-123", CatalogServiceRequest.newBuilder().build());
        assertThat(montBlancService.getValue().get(0).getServiceMeta(), hasEntry("region", "us-east"));
        assertThat(montBlancService.getValue().get(0).getServiceMeta(), not(hasKey("service")));
        assertThat(montBlancService.getValue().get(0).getServiceMeta(), not(hasKey("tags")));

        var montGrisService = consulClient.getCatalogService("mont-gris", CatalogServiceRequest.newBuilder().build());
        assertThat(montGrisService.getValue().get(0).getServiceMeta(), hasEntry("region", "us-west"));
        assertThat(montGrisService.getValue().get(0).getServiceMeta(), not(hasKey("service")));
        assertThat(montGrisService.getValue().get(0).getServiceMeta(), not(hasKey("tags")));
    }

    @CompatTest
    void labelEnvPriority(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse createContainer = createContainer(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.empty())
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_NAME", "mont-blanc"))
                .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "mont-gris"))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("mont-gris"));

        var services = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();

        assertThat(services, not(hasKey("mont-blanc")));
        assertThat(services, hasKey("mont-gris"));
    }

    @CompatTest
    void exposedMappedPorts(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer registrationContainer) {
        CreateContainerResponse createContainer = createContainer(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.empty())
                .andThen(SimpleContainerModifier.withExposedPort(ExposedPort.tcp(456)))
                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_NAME", "mont-blanc"))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        if(!registrationContainer.isInternalPorts()) {
            await.until(consulWaiter().serviceExists("mont-blanc"));
        } else {
            await.until(consulWaiter().serviceExists("mont-blanc-123"));
        }

        var services = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();

        if(!registrationContainer.isInternalPorts()) {
            assertThat(services, allOf(
                    hasKey("mont-blanc"),
                    not(hasKey("mont-blanc-456")),
                    not(hasKey("mont-blanc-123"))
            ));
        } else {
            assertThat(services, allOf(
                    not(hasKey("mont-blanc")),
                    hasKey("mont-blanc-456"),
                    hasKey("mont-blanc-123")
            ));
        }
    }

    @CompatTest
    void ignore(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer container) {
        CreateContainerResponse myOtherServiceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(665), Binding.empty()))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(666), Binding.empty()))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(667), Binding.empty()))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.udp(668), Binding.empty()))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.udp(669), Binding.empty()))
                        .andThen(SimpleContainerModifier.withEnvVar("SERVICE_665_IGNORE", ""))
                        .andThen(SimpleContainerModifier.withEnvVar("SERVICE_666_IGNORE", "0"))
                        .andThen(SimpleContainerModifier.withEnvVar("SERVICE_668_IGNORE", "1"))
        );

        CreateContainerResponse myServiceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myservice"))
        );


        dockerClient.startContainerCmd(myOtherServiceContainer.getId()).exec();
        dockerClient.startContainerCmd(myServiceContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("myservice"));

        var catalogServices = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();
        if(container.isRegistratorExplicitOnly()) {
            assertThat(catalogServices, not(hasKey(startsWith("alpine"))));
        } else {
            assertThat(catalogServices, allOf(
                    hasKey("alpine-123"),
                    hasKey("alpine-665"),
                    not(hasKey("alpine-666")),
                    hasKey("alpine-667"),
                    not(hasKey("alpine-668")),
                    hasKey("alpine-669")
            ));
            var port665Service = consulClient.getCatalogService("alpine-665", CatalogServiceRequest.newBuilder().build()).getValue().get(0);

            assertThat(port665Service.getServiceMeta(), hasEntry("ignore", ""));
        }

    }

    @NuntioTest
    void nuntioConfig(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer container) {
        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(8080), Binding.bindIp("127.0.1.8"))
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "high-hat"))
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/metadata/some-value", "abc"))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("high-hat"));

        var alpineServices = consulClient.getCatalogService("high-hat", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(alpineServices, hasSize(1));

        var service = alpineServices.get(0);

        var inspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        if (container.isInternalPorts()) {
            assertThat(service.getServiceAddress(), equalTo(inspect.findInternalIps().get("bridge")));
            assertThat(Integer.toString(service.getServicePort()), equalTo("8080"));
        } else {
            assertThat(service.getServiceAddress(), equalTo("127.0.1.8"));
            var mappedPort = inspect.findSingleContainerBinding(ExposedPort.tcp(8080)).getHostPortSpec();
            assertThat(Integer.toString(service.getServicePort()), equalTo(mappedPort));
        }
        assertThat(service.getServiceMeta(), hasEntry("some-value", "abc"));
    }

    @NuntioTest
    void nuntioConfigBeforeRegistratorCompat(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer container) {
        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(8080), Binding.bindIp("127.0.1.8"))
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "high-hat"))
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/metadata/some-value", "abc")
                                .andThen(SimpleContainerModifier.withEnvVar("SERVICE_NAME", "low-rider")))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("high-hat"));

        var alpineServices = consulClient.getCatalogService("high-hat", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(alpineServices, hasSize(1));

        var service = alpineServices.get(0);

        var inspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        if (container.isInternalPorts()) {
            assertThat(service.getServiceAddress(), equalTo(inspect.findInternalIps().get("bridge")));
            assertThat(Integer.toString(service.getServicePort()), equalTo("8080"));
        } else {
            assertThat(service.getServiceAddress(), equalTo("127.0.1.8"));
            var mappedPort = inspect.findSingleContainerBinding(ExposedPort.tcp(8080)).getHostPortSpec();
            assertThat(Integer.toString(service.getServicePort()), equalTo(mappedPort));
        }
        assertThat(service.getServiceMeta(), hasEntry("some-value", "abc"));

        var services = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();
        assertThat(services, not(hasKey("low-rider")));
    }

}
