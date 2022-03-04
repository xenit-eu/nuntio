package eu.xenit.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.ecwid.consul.v1.ConsulClient;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.engine.metrics.RegistryMetricsImpl;
import eu.xenit.nuntio.integtest.containers.NuntioContainer;
import eu.xenit.nuntio.integtest.jupiter.annotations.ContainerTests;
import eu.xenit.nuntio.integtest.jupiter.annotations.NuntioTest;
import eu.xenit.nuntio.integtest.util.SimpleContainerModifier;
import eu.xenit.nuntio.platform.docker.DockerContainerIdServiceIdentifier;
import eu.xenit.nuntio.registry.consul.ConsulProperties;
import eu.xenit.nuntio.registry.consul.ConsulRegistry;
import io.micrometer.core.instrument.Metrics;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContainerTests
public class ContainerExistingServiceTest extends ContainerBaseTest {
    private NuntioContainer nuntioNormalContainer = new NuntioContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withLive(false);

    @NuntioTest
    void matchingServiceWithDifferentName(DockerClient dockerClient, ConsulClient consulClient) {
        CreateContainerResponse serviceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIpAndPort("127.0.0.1", 123))
                        .andThen(SimpleContainerModifier.withLabel("nuntio.xenit.eu/service", "myservice"))
        );

        var platformId = new DockerContainerIdServiceIdentifier(serviceContainer.getId());
        var serviceId = ServiceIdentifier.of(platformId.getPlatformIdentifier(), ServiceBinding.fromPortAndProtocol(123, "tcp").withIp("127.0.0.1"));

        var consulRegistry = new ConsulRegistry(consulClient, new ConsulProperties(), new RegistryMetricsImpl(Metrics.globalRegistry, "consul"));
        consulRegistry.registerService(RegistryServiceDescription.builder()
                .serviceIdentifier(serviceId)
                .platformIdentifier(platformId.getPlatformIdentifier())
                .name("myservice-alt")
                .port("123")
                .build());

        await.until(consulWaiter().serviceExists("myservice-alt"));

        dockerClient.startContainerCmd(serviceContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("myservice"));

        assertThat(consulClient.getAgentServices().getValue(), not(hasKey("myservice-alt")));

    }


}
