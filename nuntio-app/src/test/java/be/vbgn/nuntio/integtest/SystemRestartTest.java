package be.vbgn.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import be.vbgn.nuntio.integtest.containers.NuntioContainer;
import be.vbgn.nuntio.integtest.containers.RegistrationContainer;
import be.vbgn.nuntio.integtest.jupiter.annotations.ContainerTests;
import be.vbgn.nuntio.integtest.jupiter.annotations.NuntioTest;
import be.vbgn.nuntio.integtest.util.SimpleContainerInspect;
import be.vbgn.nuntio.integtest.util.SimpleContainerModifier;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContainerTests
public class SystemRestartTest extends ContainerBaseTest {
    private NuntioContainer nuntio = new NuntioContainer()
            .withDindContainer(dindContainer)
            .withConsulContainer(consulContainer)
            .withNetwork(network);

    private NuntioContainer nuntioWithoutLive = new NuntioContainer()
            .withDindContainer(dindContainer)
            .withConsulContainer(consulContainer)
            .withNetwork(network)
            .withLive(false);


    @NuntioTest
    void testRestartDocker(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer registrationContainer) {
        CreateContainerResponse container = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "my-service"))
                        .andThen(SimpleContainerModifier.withRestartPolicy(RestartPolicy.unlessStoppedRestart()))
        );

        CreateContainerResponse container2 = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "my-service2"))
        );

        // First start second container so after a dockerd restart port mappings will not be the same
        dockerClient.startContainerCmd(container2.getId()).exec();
        dockerClient.startContainerCmd(container.getId()).exec();

        await.until(consulWaiter().serviceExists("my-service"));
        await.until(consulWaiter().serviceExists("my-service2"));

        SimpleContainerInspect containerInspectBefore = new SimpleContainerInspect(dockerClient.inspectContainerCmd(container.getId()));

        // Simulate full restart of docker (including hosted nuntio container)
        registrationContainer.stop();
        dindContainer.getDockerClient().restartContainerCmd(dindContainer.getContainerId()).exec();


        await.until(() -> {
            try {
                dindContainer.getDindClient().pingCmd().exec();
                return true;
            } catch(Exception e) {
                return false;
            }
        });

        // Re-assign dockerClient, because mapped port of dind has changed
        dockerClient = dindContainer.getDindClient();

        // Schedule an additional container *before* nuntio is restarted
        CreateContainerResponse markerContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "marker"))
        );
        dockerClient.startContainerCmd(markerContainer.getId()).exec();

        // Restart nuntio
        registrationContainer.start();

        await.until(consulWaiter().serviceExists("marker"));

        var myServices = consulClient.getCatalogService("my-service", CatalogServiceRequest.newBuilder().build()).getValue();

        SimpleContainerInspect containerInspectAfter = new SimpleContainerInspect(dockerClient.inspectContainerCmd(container.getId()));

        assertThat(containerInspectBefore.findSingleContainerPort(ExposedPort.tcp(80)), not(equalTo(containerInspectAfter.findSingleContainerPort(ExposedPort.tcp(80)))));

        assertThat(myServices, hasSize(1));
        assertThat(myServices.get(0).getServicePort(), equalTo(containerInspectAfter.findSingleContainerPort(ExposedPort.tcp(80))));
    }


    @NuntioTest
    void testRestartConsul(DockerClient dockerClient) {
        CreateContainerResponse container = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "my-service"))
        );

        CreateContainerResponse container2 = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "my-service2"))
        );
        dockerClient.startContainerCmd(container.getId()).exec();

        await.until(consulWaiter().serviceExists("my-service"));

        // Destroy consul container
        consulContainer.stop();

        dockerClient.startContainerCmd(container2.getId()).exec();

        consulContainer.start();

        await.until(() -> {
            try {
                consulContainer.getConsulClient().getAgentSelf();
                return true;
            } catch(Exception e) {
                return false;
            }
        });

        await.until(consulWaiter().serviceExists("my-service2"));

        CreateContainerResponse markerContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "marker"))
        );
        dockerClient.startContainerCmd(markerContainer.getId()).exec();
        await.until(consulWaiter().serviceExists("marker"));
        dockerClient.stopContainerCmd(markerContainer.getId()).exec();
        await.until(consulWaiter().serviceDoesNotExist("marker"));

        assertThat(consulContainer.getConsulClient().getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue(), hasKey("my-service"));
    }
}
