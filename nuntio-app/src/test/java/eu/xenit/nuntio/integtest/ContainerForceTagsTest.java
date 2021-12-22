package eu.xenit.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import eu.xenit.nuntio.integtest.containers.NuntioContainer;
import eu.xenit.nuntio.integtest.containers.RegistratorContainer;
import eu.xenit.nuntio.integtest.jupiter.annotations.CompatTest;
import eu.xenit.nuntio.integtest.jupiter.annotations.ContainerTests;
import eu.xenit.nuntio.integtest.util.SimpleContainerModifier;
import java.util.HashSet;
import java.util.Set;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContainerTests
public class ContainerForceTagsTest extends ContainerBaseTest {

    private NuntioContainer nuntio = new NuntioContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withRegistratorCompat(true)
            .withForcedTags(Set.of("tag1", "tag2"));

    private RegistratorContainer registrator = new RegistratorContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withForcedTags(Set.of("tag1", "tag2"));

    @CompatTest
    void forceAdditionalTags(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse myService = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myService"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_TAGS", "mytag,tag2"))
        );

        dockerClient.startContainerCmd(myService.getId()).exec();

        await.until(consulWaiter().serviceExists("myService"));

        var services = consulClient.getCatalogService("myService", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(services, hasSize(1));

        assertThat(new HashSet<>(services.get(0).getServiceTags()), containsInAnyOrder("mytag", "tag1", "tag2"));
    }

    @CompatTest
    void forceTags(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse myService = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myService"))
        );

        dockerClient.startContainerCmd(myService.getId()).exec();

        await.until(consulWaiter().serviceExists("myService"));

        var services = consulClient.getCatalogService("myService", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(services, hasSize(1));

        assertThat(services.get(0).getServiceTags(), containsInAnyOrder("tag1", "tag2"));
    }
}
