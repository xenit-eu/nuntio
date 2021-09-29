package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceHealth;
import be.vbgn.nuntio.api.platform.PlatformServiceHealth.HealthStatus;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import java.util.Optional;
import java.util.Set;
import lombok.ToString;
import lombok.Value;

@Value
public class DockerContainerServiceDescription implements PlatformServiceDescription {


    @ToString.Exclude
    InspectContainerResponse response;
    Set<PlatformServiceConfiguration> serviceConfigurations;

    @Override
    @ToString.Include(name = "identifier")
    public DockerContainerServiceIdentifier getIdentifier() {
        String fullContainerName = response.getName();
        String shortContainerName = fullContainerName.substring(fullContainerName.lastIndexOf('/') + 1);
        return new DockerContainerServiceIdentifier(shortContainerName,
                response.getId());
    }

    @Override
    @ToString.Include(name = "state")
    public PlatformServiceState getState() {
        var state = response.getState();
        if (state.getPaused()) {
            return PlatformServiceState.PAUSED;
        } else if (state.getRunning()) {
            return PlatformServiceState.RUNNING;
        } else {
            return PlatformServiceState.STOPPED;
        }
    }

    @Override
    @ToString.Include(name = "health")
    public Optional<PlatformServiceHealth> getHealth() {
        var healthState = Optional.ofNullable(response.getState())
                .map(ContainerState::getHealth);
        String healthStatus = healthState.map(HealthState::getStatus)
                .orElse("none");
        String checkLogs = healthState.map(HealthState::getLog)
                .filter(logs -> !logs.isEmpty())
                .map(logs -> logs.get(logs.size() - 1))
                .map(logEntry -> "exitCode=" + logEntry.getExitCodeLong() + "\n" + logEntry.getOutput())
                .orElse("No logs");
        switch (healthStatus) {
            case "starting":
                return Optional.of(new PlatformServiceHealth(HealthStatus.STARTING, checkLogs));
            case "healthy":
                return Optional.of(new PlatformServiceHealth(HealthStatus.HEALTHY, checkLogs));
            case "unhealthy":
                return Optional.of(new PlatformServiceHealth(HealthStatus.UNHEALTHY, checkLogs));
            case "none":
            default:
                return Optional.empty();
        }
    }
}
