package be.vbgn.nuntio.integtest.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.time.Duration;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

@Getter
@Setter
public class DindContainer extends GenericContainer<DindContainer> {

    private int daemonPort = 2375;

    public DindContainer() {
        super("docker.io/library/docker:dind");
        setPrivilegedMode(true);

        addEnv("DOCKER_TLS_CERTDIR", ""); // Disable docker TLS

        withCreateContainerCmdModifier(createContainerCmd -> {
            HealthCheck healthCheck = new HealthCheck();
            healthCheck.withTest(Arrays.asList("CMD", "docker", "info"));
            healthCheck.withInterval(Duration.ofSeconds(1).toNanos());
            createContainerCmd.withHealthcheck(healthCheck);
        });
        setStartupCheckStrategy(new HealthcheckPassingStartupCheckStrategy());
        if(System.getProperty("dind.root") != null) {
            withFileSystemBind(System.getProperty("dind.root"), "/var/lib/docker", BindMode.READ_WRITE);
        }
    }

    @Override
    protected void configure() {
        setCommand(
                "dockerd",
                "--tls=false",
                "--host=tcp://0.0.0.0:"+getDaemonPort(),
                "--host=unix:///var/run/docker.sock"
        );
        addExposedPort(getDaemonPort());
    }

    public DockerClient getDindClient() {
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://"+getHost()+":"+getMappedPort(getDaemonPort()))
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .build();

        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }
}
