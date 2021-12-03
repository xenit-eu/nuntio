package be.vbgn.nuntio.integtest.containers;

import be.vbgn.nuntio.integtest.util.SimpleContainerModifier;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.BindOptions;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.ResourceReaper;

@Getter
@Setter
public class DindContainer extends GenericContainer<DindContainer> {
    private int daemonPort = 2375;
    private static final DindContainerSharedVolume sharedVolume = new DindContainerSharedVolume();

    private static class DindContainerSharedVolume {
        private CreateVolumeResponse createVolumeResponse;

        public CreateVolumeResponse create(DockerClient dockerClient) {
            if(createVolumeResponse == null) {
                createVolumeResponse = dockerClient.createVolumeCmd()
                        // Mark with testcontainer labels so ryuk will get rid of it after tests have finished
                        .withLabels(DockerClientFactory.DEFAULT_LABELS)
                        .exec();
            }
            return createVolumeResponse;
        }

    }

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

        CreateVolumeResponse createdVolume = sharedVolume.create(getDockerClient());

        withCreateContainerCmdModifier(SimpleContainerModifier.withMount(new Mount()
                .withType(MountType.VOLUME)
                .withSource(createdVolume.getName())
                .withTarget("/var/lib/docker")
        ));
    }

    @Override
    public InspectContainerResponse getContainerInfo() {
        // Always inspect so our data is up-to-date after restarting the container outside of testcontainers control
        return getDockerClient().inspectContainerCmd(getContainerId()).exec();
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
