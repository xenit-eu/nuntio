package eu.xenit.nuntio.integtest.containers;

import com.github.dockerjava.api.model.HealthCheck;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.testcontainers.containers.GenericContainer;

@EqualsAndHashCode(callSuper = true)
@Getter
public class NuntioContainer extends GenericContainer<NuntioContainer> implements RegistrationContainer<NuntioContainer> {

    private DindContainer dindContainer;
    private ConsulContainer consulContainer;

    public NuntioContainer() {
        super(System.getProperty("nuntio.image"));
        withEnv("LOGGING_LEVEL_EU_XENIT_NUNTIO", "DEBUG");
        setStartupCheckStrategy(new HealthcheckPassingStartupCheckStrategy());
        withCreateContainerCmdModifier(createContainerCmd -> {
            HealthCheck healthCheck = new HealthCheck();
            healthCheck.withInterval(Duration.ofSeconds(2).toNanos());
            healthCheck.withStartPeriod(Duration.ofSeconds(20).toNanos());
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
    public NuntioContainer withForcedTags(Set<String> forcedTags) {
        return withEnv("NUNTIO_ENGINE_FORCEDTAGS", String.join(",", forcedTags));
    }

    @Override
    public boolean isInternalPorts() {
        return getEnvMap().getOrDefault("NUNTIO_DOCKER_BIND", "").equals("INTERNAL");
    }

    @Override
    public boolean isRegistratorExplicitOnly() {
        return getEnvMap().getOrDefault("NUNTIO_DOCKER_REGISTRATORCOMPAT_EXPLICIT", "").equals(Boolean.TRUE.toString());
    }

    @Override
    public Set<String> getForcedTags() {
        return Arrays.stream(getEnvMap().getOrDefault("NUNTIO_ENGINE_FORCEDTAGS", "").split(","))
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toSet());
    }

    public NuntioContainer withLive(boolean enabled) {
        return withEnv("NUNTIO_ENGINE_LIVE_ENABLED", Boolean.toString(enabled));
    }

    public NuntioContainer withAntiEntropy(boolean enabled) {
        return withEnv("NUNTIO_ENGINE_ANTIENTROPY_ENABLED", Boolean.toString(enabled));
    }
}
