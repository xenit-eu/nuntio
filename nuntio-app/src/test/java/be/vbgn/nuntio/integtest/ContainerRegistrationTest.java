package be.vbgn.nuntio.integtest;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

import be.vbgn.nuntio.integtest.annotations.RegistratorCompat;
import be.vbgn.nuntio.integtest.util.ConsulWaiter;
import be.vbgn.nuntio.integtest.util.SimpleContainerInspect;
import be.vbgn.nuntio.integtest.util.SimpleContainerModifier;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContainerRegistrationTest {
    private DockerClient dockerClient;
    private ConsulClient consulClient;
    private ConsulWaiter consulWaiter;

    @BeforeEach
    void createDockerClient() throws InterruptedException {
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://"+System.getProperty("dind.host")+":"+System.getProperty("dind.tcp.2375"))
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .build();

        dockerClient = DockerClientImpl.getInstance(clientConfig, httpClient);

        // Check that docker is reachable
        dockerClient.pingCmd().exec();

        // Prepare alpine image if it is not available
        dockerClient.pullImageCmd("library/alpine")
                .withTag("latest")
                .withRegistry("docker.io")
                .start()
                .awaitCompletion();

    }

    @AfterEach
    void removeDockerContainers() {
        // Clean up all running containers
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
                });
    }

    @BeforeEach
    void createConsulClient() {
        consulClient = new ConsulClient(System.getProperty("consul.host"), Integer.getInteger("consul.tcp.8500"));

        // Check that consul is reachable
        consulClient.getAgentSelf();
        consulWaiter = new ConsulWaiter(consulClient);
    }

    private CreateContainerResponse createContainer(SimpleContainerModifier containerModifier) {
        var cmd = dockerClient.createContainerCmd("alpine")
                .withCmd("sleep", "infinity");
        containerModifier.apply(cmd);
        return cmd.exec();
    }


    @Test
    @RegistratorCompat
    void createContainerWithDefaultRegistratorConfig() throws Exception {
         createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("service_name", "alpine2"))
        );

        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "alpine"))
        );


        dockerClient.startContainerCmd(createContainer.getId()).exec();
        var inspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        var mappedPort = inspect.findSingleContainerBinding(ExposedPort.tcp(123)).getHostPortSpec();

        await().until(consulWaiter.serviceExists("alpine"));

        var alpineServices = consulClient.getCatalogService("alpine", CatalogServiceRequest.newBuilder().build())
                .getValue();

        assertThat(alpineServices, hasSize(1));

        var service = alpineServices.get(0);


        assertThat(service.getAddress(), equalTo("127.0.0.1"));
        assertThat(service.getServiceAddress(), anyOf(equalTo("127.0.0.1"), emptyString()));
        assertThat(Integer.toString(service.getServicePort()), equalTo(mappedPort));

        assertFalse(consulWaiter.serviceExists("alpine2").call());
    }

    @Test
    @RegistratorCompat
    void createContainerWithEnvvarMultiplePortsRegistratorConfig() {
        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindPort(666))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(444), Binding.empty()))
                        .andThen(SimpleContainerModifier.withEnvVar("SERVICE_NAME", "mont-blanc"))
                        .andThen(SimpleContainerModifier.withEnvVar("SERVICE_444_TAGS", "tls"))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();

        await().until(consulWaiter.serviceExists("mont-blanc-123"));

        var services = consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue();

        assertThat(services, hasKey("mont-blanc-123"));
        assertThat(services, hasKey("mont-blanc-444"));
        assertThat(services, not(hasKey("mont-blanc")));
    }

    @Test
    void createContainerWithNuntioConfig() {
        CreateContainerResponse createContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(8080), Binding.bindIp("127.0.1.8"))
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/service", "high-hat"))
                        .andThen(SimpleContainerModifier.withLabel("nuntio.vbgn.be/metadata/some-value", "abc"))
        );

        dockerClient.startContainerCmd(createContainer.getId()).exec();
        var inspect = new SimpleContainerInspect(dockerClient.inspectContainerCmd(createContainer.getId()).exec());
        var mappedPort = inspect.findSingleContainerBinding(ExposedPort.tcp(8080)).getHostPortSpec();

        await().until(consulWaiter.serviceExists("high-hat"));

        var alpineServices = consulClient.getCatalogService("high-hat", CatalogServiceRequest.newBuilder().build()).getValue();

        assertThat(alpineServices, hasSize(1));

        var service = alpineServices.get(0);

        assertThat(service.getServiceAddress(), equalTo("127.0.1.8"));
        assertThat(Integer.toString(service.getServicePort()), equalTo(mappedPort));
    }



}
