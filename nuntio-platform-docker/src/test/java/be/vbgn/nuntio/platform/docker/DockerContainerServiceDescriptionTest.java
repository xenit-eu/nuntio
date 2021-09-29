package be.vbgn.nuntio.platform.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vbgn.nuntio.api.platform.PlatformServiceHealth;
import be.vbgn.nuntio.api.platform.PlatformServiceHealth.HealthStatus;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.HealthStateLog;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DockerContainerServiceDescriptionTest {

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    @Test
    void getIdentifier() {
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        Mockito.doReturn("/my-funny-name").when(inspectContainerResponse).getName();
        Mockito.doReturn("aabbcc").when(inspectContainerResponse).getId();

        var description = new DockerContainerServiceDescription(inspectContainerResponse, Collections.emptySet());

        assertEquals("my-funny-name", description.getIdentifier().getContainerName());
        assertEquals("aabbcc", description.getIdentifier().getContainerId());

        assertEquals(new DockerContainerIdServiceIdentifier("aabbcc").getSharedIdentifier(),
                description.getIdentifier().getSharedIdentifier());
    }

    @Test
    void getState() {
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        ContainerState containerState = mock(ContainerState.class);
        Mockito.doReturn(containerState).when(inspectContainerResponse).getState();
        Mockito.doReturn(true).when(containerState).getRunning();
        Mockito.doReturn(false).when(containerState).getPaused();

        var description = new DockerContainerServiceDescription(inspectContainerResponse, Collections.emptySet());

        assertEquals(PlatformServiceState.RUNNING, description.getState());

        Mockito.doReturn(false).when(containerState).getRunning();
        Mockito.doReturn(true).when(containerState).getPaused();

        assertEquals(PlatformServiceState.PAUSED, description.getState());

        Mockito.doReturn(false).when(containerState).getPaused();

        assertEquals(PlatformServiceState.STOPPED, description.getState());
    }

    private static HealthStateLog logEntry(long exitCode, String output) {
        HealthStateLog healthStateLog = mock(HealthStateLog.class);
        Mockito.doReturn(exitCode).when(healthStateLog).getExitCodeLong();
        Mockito.doReturn(output).when(healthStateLog).getOutput();
        return healthStateLog;
    }

    @Test
    void getHealth() {
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        ContainerState containerState = mock(ContainerState.class);
        Mockito.doReturn(containerState).when(inspectContainerResponse).getState();
        HealthState healthState = mock(HealthState.class);
        Mockito.doReturn(null).when(containerState).getHealth();

        var description = new DockerContainerServiceDescription(inspectContainerResponse, Collections.emptySet());

        assertEquals(Optional.empty(), description.getHealth());

        Mockito.doReturn(healthState).when(containerState).getHealth();
        Mockito.doReturn("none").when(healthState).getStatus();
        Mockito.doReturn(null).when(healthState).getLog();

        assertEquals(Optional.empty(), description.getHealth());

        Mockito.doReturn("starting").when(healthState).getStatus();

        assertEquals(Optional.of(new PlatformServiceHealth(HealthStatus.STARTING, "No logs")), description.getHealth());

        Mockito.doReturn("healthy").when(healthState).getStatus();
        Mockito.doReturn(Arrays.asList(
                logEntry(0, "Bla"),
                logEntry(0, "234"),
                logEntry(0, "567")
        )).when(healthState).getLog();

        assertEquals(Optional.of(new PlatformServiceHealth(HealthStatus.HEALTHY, "exitCode=0\n567")),
                description.getHealth());

        Mockito.doReturn("unhealthy").when(healthState).getStatus();
        Mockito.doReturn(Arrays.asList(
                logEntry(112, "567")
        )).when(healthState).getLog();

        assertEquals(Optional.of(new PlatformServiceHealth(HealthStatus.UNHEALTHY, "exitCode=112\n567")),
                description.getHealth());

        Mockito.doReturn(List.of()).when(healthState).getLog();

        assertEquals(Optional.of(new PlatformServiceHealth(HealthStatus.UNHEALTHY, "No logs")),
                description.getHealth());
    }

}
