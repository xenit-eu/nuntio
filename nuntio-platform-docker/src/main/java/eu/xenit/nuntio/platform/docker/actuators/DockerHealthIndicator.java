package eu.xenit.nuntio.platform.docker.actuators;

import com.github.dockerjava.api.DockerClient;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@AllArgsConstructor
public class DockerHealthIndicator implements HealthIndicator {

    private DockerClient dockerClient;

    @Override
    public Health health() {
        try {
            dockerClient.pingCmd().exec();
            return Health.up().build();
        } catch(Exception e) {
            return Health.down(e).build();
        }
    }
}
