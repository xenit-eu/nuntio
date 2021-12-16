package eu.xenit.nuntio.integtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.Check;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.xenit.nuntio.integtest.containers.NuntioContainer;
import eu.xenit.nuntio.integtest.containers.RegistratorContainer;
import eu.xenit.nuntio.integtest.jupiter.annotations.CompatTest;
import eu.xenit.nuntio.integtest.jupiter.annotations.ContainerTests;
import eu.xenit.nuntio.integtest.util.SimpleContainerModifier;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.testcontainers.junit.jupiter.Testcontainers;

@ContainerTests
@Testcontainers
public class ContainerChecksTest extends ContainerBaseTest {

    private RegistratorContainer registrator = new RegistratorContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withRegistratorExplicitOnly(true);


    private NuntioContainer nuntioContainer = new NuntioContainer()
            .withNetwork(network)
            .withConsulContainer(consulContainer)
            .withDindContainer(dindContainer)
            .withRegistratorCompat(true)
            .withRegistratorExplicitOnly(true);

    private List<JsonObject> getAgentChecks(UrlParameters urlParameters, String typeFilter) {
        var agentChecksResponse = consulContainer.getConsulRawClient()
                .makeGetRequest("/v1/agent/checks", urlParameters);

        JsonElement agentChecks = JsonParser.parseString(agentChecksResponse.getContent());

        return agentChecks.getAsJsonObject().entrySet().stream()
                .map(Entry::getValue)
                .map(JsonElement::getAsJsonObject)
                .filter(object -> object.get("Type").getAsString().equals(typeFilter))
                .collect(Collectors.toUnmodifiableList());
    }

    private Check waitForCheckOutput(String checkId) {
        await.until(consulWaiter().checkHasOutput(checkId));
        return consulContainer.getConsulClient().getAgentChecks().getValue().get(checkId);
    }

    @CompatTest
    void httpCheck(DockerClient dockerClient) {
        CreateContainerResponse serviceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myservice"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_CHECK_HTTP", "/health"))
        );

        dockerClient.startContainerCmd(serviceContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("myservice"));
        await.until(consulWaiter().serviceHasChecks("myservice"));

        var httpChecks = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice\""), "http");

        assertThat(httpChecks, hasSize(1));
    }

    @CompatTest
    void httpsCheck(DockerClient dockerClient) {
        CreateContainerResponse serviceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myservice"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_CHECK_HTTPS", "/health"))
        );

        dockerClient.startContainerCmd(serviceContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("myservice"));
        await.until(consulWaiter().serviceHasChecks("myservice"));

        var httpChecks = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice\""), "http");

        assertThat(httpChecks, hasSize(1));
    }

    @CompatTest
    void mixedChecks(ConsulClient consulClient, DockerClient dockerClient) {
        CreateContainerResponse serviceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(124), Binding.empty()))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(125), Binding.empty()))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(126), Binding.empty()))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myservice"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_123_CHECK_HTTPS", "/health"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_124_CHECK_TCP", "true"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_125_CHECK_HTTP", "/healtz"))
        );

        dockerClient.startContainerCmd(serviceContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("myservice-123"));
        await.until(consulWaiter().serviceExists("myservice-124"));
        await.until(consulWaiter().serviceExists("myservice-125"));
        await.until(consulWaiter().serviceExists("myservice-126"));

        var httpChecks123 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-123\""), "http");
        assertThat(httpChecks123, hasSize(1));
        assertThat(waitForCheckOutput(httpChecks123.get(0).get("CheckID").getAsString()).getOutput(), stringContainsInOrder("https://", "/health"));

        var tcpChecks123 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-123\""), "tcp");
        assertThat(tcpChecks123, hasSize(0));
        var httpChecks124 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-124\""), "http");
        assertThat(httpChecks124, hasSize(0));
        var tcpChecks124 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-124\""), "tcp");
        assertThat(tcpChecks124, hasSize(1));
        assertThat(waitForCheckOutput(tcpChecks124.get(0).get("CheckID").getAsString()).getOutput(), startsWith("dial tcp"));
        var httpChecks125 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-125\""), "http");
        assertThat(httpChecks125, hasSize(1));
        assertThat(waitForCheckOutput(httpChecks125.get(0).get("CheckID").getAsString()).getOutput(), stringContainsInOrder("http://", "/healtz"));
        var tcpChecks125 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-125\""), "tcp");
        assertThat(tcpChecks125, hasSize(0));
        var httpChecks126 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-126\""), "http");
        assertThat(httpChecks126, hasSize(0));
        var tcpChecks126 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-126\""), "tcp");
        assertThat(tcpChecks126, hasSize(0));
    }

    @CompatTest
    void checkPriority(DockerClient dockerClient) {
        CreateContainerResponse serviceContainer = createContainer(
                SimpleContainerModifier.withPortBinding(ExposedPort.tcp(123), Binding.bindIp("127.0.0.1"))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(124), Binding.empty()))
                        .andThen(SimpleContainerModifier.withPortBinding(ExposedPort.tcp(125), Binding.empty()))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_NAME", "myservice"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_CHECK_HTTPS", "/health"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_124_CHECK_TCP", "true"))
                        .andThen(SimpleContainerModifier.withLabel("SERVICE_125_CHECK_HTTP", "/healtz"))
        );

        // Checks are prioritized in order HTTP -> HTTPS -> TCP
        // so the global SERVICE_CHECK_HTTPS will overwrite the specific SERVICE_124_CHECK_TCP and will be overwritten by SERVICE_125_CHECK_HTTP.

        dockerClient.startContainerCmd(serviceContainer.getId()).exec();

        await.until(consulWaiter().serviceExists("myservice-123"));
        await.until(consulWaiter().serviceExists("myservice-124"));
        await.until(consulWaiter().serviceExists("myservice-125"));

        var httpChecks123 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-123\""), "http");
        assertThat(httpChecks123, hasSize(1));
        assertThat(waitForCheckOutput(httpChecks123.get(0).get("CheckID").getAsString()).getOutput(), stringContainsInOrder("https://", "/health"));
        var tcpChecks123 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-123\""), "tcp");
        assertThat(tcpChecks123, hasSize(0));
        var httpChecks124 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-124\""), "http");
        assertThat(httpChecks124, hasSize(1));
        assertThat(waitForCheckOutput(httpChecks124.get(0).get("CheckID").getAsString()).getOutput(), stringContainsInOrder("https://", "/health"));
        var tcpChecks124 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-124\""), "tcp");
        assertThat(tcpChecks124, hasSize(0));
        var httpChecks125 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-125\""), "http");
        assertThat(httpChecks125, hasSize(1));
        assertThat(waitForCheckOutput(httpChecks125.get(0).get("CheckID").getAsString()).getOutput(), stringContainsInOrder("http://", "/healtz"));
        var tcpChecks125 = getAgentChecks(new SingleUrlParameters("filter", "ServiceName==\"myservice-125\""), "tcp");
        assertThat(tcpChecks125, hasSize(0));
    }
}
