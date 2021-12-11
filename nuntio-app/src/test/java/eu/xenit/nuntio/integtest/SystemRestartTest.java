package eu.xenit.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.github.dockerjava.api.exception.ConflictException;
import eu.xenit.nuntio.integtest.containers.NuntioContainer;
import eu.xenit.nuntio.integtest.containers.RegistrationContainer;
import eu.xenit.nuntio.integtest.jupiter.annotations.ContainerTests;
import eu.xenit.nuntio.integtest.jupiter.annotations.NuntioTest;
import eu.xenit.nuntio.integtest.util.SimpleContainerInspect;
import eu.xenit.nuntio.integtest.util.SimpleContainerModifier;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.MatcherAssert;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContainerTests
@Slf4j
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
    void testRestartDocker(DockerClient dockerClient, ConsulClient consulClient, RegistrationContainer registrationContainer)
            throws InterruptedException {
        CreateContainerResponse container = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "my-service"))
                        .andThen(SimpleContainerModifier.withRestartPolicy(RestartPolicy.unlessStoppedRestart()))
        );

        CreateContainerResponse container2 = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "my-service2"))
        );

        // First start second container so after a dockerd restart port mappings will not be the same
        dockerClient.startContainerCmd(container2.getId()).exec();
        dockerClient.startContainerCmd(container.getId()).exec();

        await.until(consulWaiter().serviceExists("my-service"));
        await.until(consulWaiter().serviceExists("my-service2"));

        SimpleContainerInspect containerInspectBefore = new SimpleContainerInspect(dockerClient.inspectContainerCmd(container.getId()));

        // Simulate full restart of docker (including hosted nuntio container)
        registrationContainer.stop();
        for(int i = 0; i < 5; i++) {
            dindContainer.getDockerClient().stopContainerCmd(dindContainer.getContainerId()).exec();
            Thread.sleep(Duration.ofSeconds(5).toMillis());
            try {
                dindContainer.getDockerClient().killContainerCmd(dindContainer.getContainerId()).exec();
                Thread.sleep(Duration.ofSeconds(2).toMillis());
            } catch(ConflictException e) {
                // No-op, container is already dead
            }
            dindContainer.getDockerClient().startContainerCmd(dindContainer.getContainerId()).exec();

            try {
                await.until(() -> {
                    try {
                        dindContainer.getDindClient().pingCmd().exec();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
                break;
            } catch (ConditionTimeoutException e) {
                log.info("Docker did not come up properly. Retrying container restart (attempt {})", i);
            }
        }

        // Re-assign dockerClient, because mapped port of dind has changed
        dockerClient = dindContainer.getDindClient();

        // Schedule an additional container *before* nuntio is restarted
        CreateContainerResponse container3 = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "my-service3"))
        );
        dockerClient.startContainerCmd(container3.getId()).exec();

        // Restart nuntio
        registrationContainer.start();

        waitForFullCycle();

        var myServices = consulClient.getCatalogService("my-service", CatalogServiceRequest.newBuilder().build()).getValue();

        SimpleContainerInspect containerInspectAfter = new SimpleContainerInspect(dockerClient.inspectContainerCmd(container.getId()));

        assertThat(containerInspectBefore.findSingleContainerPort(ExposedPort.tcp(80)), not(equalTo(containerInspectAfter.findSingleContainerPort(ExposedPort.tcp(80)))));

        assertThat(myServices, hasSize(1));
        assertThat(myServices.get(0).getServicePort(), equalTo(containerInspectAfter.findSingleContainerPort(ExposedPort.tcp(80))));

        assertThat(consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue(), hasKey("my-service3"));
    }


    @NuntioTest
    void testRestartConsul(DockerClient dockerClient) {
        CreateContainerResponse container = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "my-service"))
        );

        CreateContainerResponse container2 = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(80), Binding.empty())
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "my-service2"))
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

        waitForFullCycle();

        await.until(consulWaiter().serviceExists("my-service"));

        MatcherAssert.assertThat(consulContainer.getConsulClient().getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue(), hasKey("my-service"));
    }
}
