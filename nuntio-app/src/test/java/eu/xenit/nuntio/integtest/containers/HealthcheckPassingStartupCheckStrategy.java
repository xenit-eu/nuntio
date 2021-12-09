package eu.xenit.nuntio.integtest.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

public class HealthcheckPassingStartupCheckStrategy extends StartupCheckStrategy {

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        ContainerState state = getCurrentState(dockerClient, containerId);
        switch(state.getHealth().getStatus()) {
            case "healthy":
                return StartupStatus.SUCCESSFUL;
            case "unhealthy":
                return StartupStatus.FAILED;
            default:
                return StartupStatus.NOT_YET_KNOWN;
        }
    }
}
