package be.vbgn.nuntio.integtest.containers;

import com.github.dockerjava.api.model.HealthCheck;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.testcontainers.containers.GenericContainer;

@EqualsAndHashCode(callSuper = true)
@Getter
public class NuntioContainer extends GenericContainer<NuntioContainer> implements RegistrationContainer<NuntioContainer> {

    private DindContainer dindContainer;
    private ConsulContainer consulContainer;

    public NuntioContainer() {
        super("hub.xenit.eu/public/nuntio:latest");
        setStartupCheckStrategy(new HealthcheckPassingStartupCheckStrategy());
        withCreateContainerCmdModifier(createContainerCmd -> {
            HealthCheck healthCheck = new HealthCheck();
            healthCheck.withInterval(Duration.ofSeconds(1).toNanos());
            healthCheck.withStartPeriod(Duration.ofSeconds(10).toNanos());
            createContainerCmd.withHealthcheck(healthCheck);
        });
    }

    public NuntioContainer withDindContainer(DindContainer dindContainer) {
        dependsOn(dindContainer);
        this.dindContainer = dindContainer;
        return this;
    }

    public NuntioContainer withConsulContainer(ConsulContainer consulContainer) {
        dependsOn(consulContainer);
        this.consulContainer = consulContainer;
        return this;
    }


    @Override
    protected void configure() {
        if(dindContainer != null) {
            addEnv("NUNTIO_DOCKER_DAEMON_HOST", "tcp://"+dindContainer.getNetworkAliases().get(0)+":"+dindContainer.getDaemonPort());
        }
        if(consulContainer != null) {
            addEnv("NUNTIO_CONSUL_HOST", consulContainer.getNetworkAliases().get(0));
            addEnv("NUNTIO_CONSUL_PORT", ""+consulContainer.getClientPort());
        }
    }

    public NuntioContainer withRegistratorCompat(boolean registratorCompat) {
        return withEnv("NUNTIO_DOCKER_REGISTRATORCOMPAT_ENABLED", Boolean.toString(registratorCompat));
    }

    @Override
    public NuntioContainer withInternalPorts(boolean internalPorts) {
        return withEnv("NUNTIO_DOCKER_BIND", internalPorts?"INTERNAL": "PUBLISHED");
    }

    @Override
    public NuntioContainer withRegistratorExplicitOnly(boolean explicitOnly) {
        return withEnv("NUNTIO_DOCKER_REGISTRATORCOMPAT_EXPLICIT", Boolean.toString(explicitOnly));
    }

    @Override
    public boolean isInternalPorts() {
        return getEnvMap().getOrDefault("NUNTIO_DOCKER_BIND", "").equals("INTERNAL");
    }

    @Override
    public boolean isRegistratorExplicitOnly() {
        return getEnvMap().getOrDefault("NUNTIO_DOCKER_REGISTRATORCOMPAT_EXPLICIT", "").equals(Boolean.TRUE.toString());
    }
}
