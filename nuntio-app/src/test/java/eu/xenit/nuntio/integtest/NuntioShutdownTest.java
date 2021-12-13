package eu.xenit.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import eu.xenit.nuntio.integtest.containers.NuntioContainer;
import eu.xenit.nuntio.integtest.jupiter.annotations.ContainerTests;
import eu.xenit.nuntio.integtest.jupiter.annotations.NuntioTest;
import eu.xenit.nuntio.integtest.util.SimpleContainerModifier;
import org.testcontainers.junit.jupiter.Testcontainers;

@ContainerTests
@Testcontainers
public class NuntioShutdownTest extends ContainerBaseTest {
    private NuntioContainer nuntio = new NuntioContainer()
            .withDindContainer(dindContainer)
            .withConsulContainer(consulContainer)
            .withNetwork(network)
            .withEnv("NUNTIO_ENGINE_SHUTDOWNMODE", "UNREGISTER_SERVICES");

    @NuntioTest
    void unregisterServicesOnShutdown(DockerClient dockerClient, ConsulClient consulClient, NuntioContainer container) {
        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "test-service")
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty()))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("test-service"));
        var consulService= consulClient.getCatalogService("test-service", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(consulService, hasSize(1));

        // Stop container without destroying it
        container.getDockerClient().stopContainerCmd(container.getContainerId()).exec();
        container.getDockerClient().waitContainerCmd(container.getContainerId()).start().awaitStatusCode();

        // After container stop has completed, services are immediately unregistered

        var services= consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();

        assertThat(services, not(hasKey("test-service")));
    }

}
