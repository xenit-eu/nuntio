package be.vbgn.nuntio.integtest;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import be.vbgn.nuntio.integtest.containers.NuntioContainer;
import be.vbgn.nuntio.integtest.jupiter.annotations.CompatTest;
import be.vbgn.nuntio.integtest.jupiter.annotations.ContainerTests;
import be.vbgn.nuntio.integtest.util.SimpleContainerInspect;
import be.vbgn.nuntio.integtest.util.SimpleContainerModifier;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import java.time.Duration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContainerTests
public class ContainerRestartTest extends ContainerBaseTest {
    private NuntioContainer nuntioWithoutLive = new NuntioContainer()
            .withDindContainer(dindContainer)
            .withConsulContainer(consulContainer)
            .withNetwork(network)
            .withLive(false);

    private NuntioContainer nuntioWithoutAntiEntropy = new NuntioContainer()
            .withDindContainer(dindContainer)
            .withConsulContainer(consulContainer)
            .withNetwork(network)
            .withAntiEntropy(false);


    @CompatTest
    void testReregistersRestartedContainer(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "test-service")
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty()))
        );

        // This container is just to be registered at a certain point in time to
        // mark that we have passed the restart of the existing container
        CreateContainerResponse markerContainer = createContainer(
                SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "marker-service")
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty()))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await().timeout(Duration.ofMinutes(2)).until(consulWaiter().serviceExists("test-service"));
        var consulService= consulClient.getCatalogService("test-service", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(consulService, hasSize(1));

        var originalInspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        var originalServicePort = Integer.parseInt(originalInspect.findSingleContainerBinding(ExposedPort.tcp(80)).getHostPortSpec());

        assertThat(consulService.get(0).getServicePort(), equalTo(originalServicePort));


        dockerClient.restartContainerCmd(createContainer.getId()).exec();

        var newInspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        var newServicePort = Integer.parseInt(newInspect.findSingleContainerBinding(ExposedPort.tcp(80)).getHostPortSpec());

        assertThat(newServicePort, not(equalTo(originalServicePort)));

        // Wait for a whole cycle of service appearing and disappearing to be sure that our anti-entropy has completed a full run
        dockerClient.startContainerCmd(markerContainer.getId()).exec();
        await().timeout(Duration.ofMinutes(2)).until(consulWaiter().serviceExists("marker-service"));
        dockerClient.stopContainerCmd(markerContainer.getId()).exec();
        await().timeout(Duration.ofMinutes(2)).until(consulWaiter().serviceDoesNotExist("marker-service"));

        var newConsulService = consulClient.getCatalogService("test-service", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(newConsulService, hasSize(1));

        assertThat(newConsulService.get(0).getServicePort(), equalTo(newServicePort));

    }

}
