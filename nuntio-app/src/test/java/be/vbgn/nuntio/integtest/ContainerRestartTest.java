package be.vbgn.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import be.vbgn.nuntio.integtest.containers.NuntioContainer;
import be.vbgn.nuntio.integtest.jupiter.annotations.ContainerTests;
import be.vbgn.nuntio.integtest.jupiter.annotations.NuntioTest;
import be.vbgn.nuntio.integtest.util.SimpleContainerInspect;
import be.vbgn.nuntio.integtest.util.SimpleContainerModifier;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
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


    @NuntioTest
    void testReregistersRestartedContainer(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "test-service")
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty()))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("test-service"));
        var consulService= consulClient.getCatalogService("test-service", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(consulService, hasSize(1));

        var originalInspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        var originalServicePort = Integer.parseInt(originalInspect.findSingleContainerBinding(ExposedPort.tcp(80)).getHostPortSpec());

        assertThat(consulService.get(0).getServicePort(), equalTo(originalServicePort));


        dockerClient.restartContainerCmd(createContainer.getId()).exec();

        var newInspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        var newServicePort = Integer.parseInt(newInspect.findSingleContainerBinding(ExposedPort.tcp(80)).getHostPortSpec());

        assertThat(newServicePort, not(equalTo(originalServicePort)));

        waitForFullCycle();

        var newConsulService = consulClient.getCatalogService("test-service", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(newConsulService, hasSize(1));

        assertThat(newConsulService.get(0).getServicePort(), equalTo(newServicePort));
    }

}
