package eu.xenit.nuntio.integtest.containers;

import com.ecwid.consul.v1.ConsulClient;
import com.github.dockerjava.api.model.HealthCheck;
import java.time.Duration;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import org.testcontainers.containers.GenericContainer;

@Getter
public class ConsulContainer extends GenericContainer<ConsulContainer> {
    private int clientPort = 8500;

    @Setter
    private String advertiseAddress = "127.0.0.1";

    public ConsulContainer() {
        super("docker.io/library/consul:latest");
        setCommand("agent", "-dev", "-client=0.0.0.0");
        setStartupCheckStrategy(new HealthcheckPassingStartupCheckStrategy());
        withCreateContainerCmdModifier(createContainerCmd -> {
            HealthCheck healthCheck = new HealthCheck();
            healthCheck.withTest(Arrays.asList("CMD", "consul", "info"));
            healthCheck.withInterval(Duration.ofSeconds(1).toNanos());
            healthCheck.withStartPeriod(Duration.ofSeconds(10).toNanos());
            createContainerCmd.withHealthcheck(healthCheck);
        });
    }

    @Override
    protected void configure() {
        addExposedPort(getClientPort());
        setCommand("agent", "-dev", "-client=0.0.0.0", "-advertise="+getAdvertiseAddress());
    }

    public ConsulClient getConsulClient() {
        return new ConsulClient(getHost(), getMappedPort(getClientPort()));
    }
}
