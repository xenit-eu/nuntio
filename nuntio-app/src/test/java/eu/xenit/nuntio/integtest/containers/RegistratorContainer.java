package eu.xenit.nuntio.integtest.containers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import org.testcontainers.containers.GenericContainer;

@EqualsAndHashCode(callSuper = true)
public class RegistratorContainer extends GenericContainer<RegistratorContainer> implements RegistrationContainer<RegistratorContainer> {

    private DindContainer dindContainer;
    private ConsulContainer consulContainer;
    private boolean internalPorts;
    private boolean explicitOnly;
    private Set<String> forcedTags;

    public RegistratorContainer() {
        super("hub.xenit.eu/public/registrator:7.1");
    }

    @Override
    public RegistratorContainer withDindContainer(DindContainer dindContainer) {
        dependsOn(dindContainer);
        this.dindContainer = dindContainer;
        return this;
    }

    @Override
    public RegistratorContainer withConsulContainer(ConsulContainer consulContainer) {
        dependsOn(consulContainer);
        this.consulContainer = consulContainer;
        return this;
    }

    @Override
    public RegistratorContainer withInternalPorts(boolean internalPorts) {
        this.internalPorts = internalPorts;
        return this;
    }

    @Override
    protected void configure() {
        List<String> commandParams = new ArrayList<>();
        if(dindContainer != null) {
            addEnv("DOCKER_HOST", "tcp://"+dindContainer.getNetworkAliases().get(0)+":"+dindContainer.getDaemonPort());
        }
        if(internalPorts) {
            commandParams.add("-internal");
        }
        if(explicitOnly) {
            commandParams.add("-explicit");
        }

        if(forcedTags != null && !forcedTags.isEmpty()) {
            commandParams.add("-tags");
            commandParams.add(String.join(",", forcedTags));
        }


        if(consulContainer != null) {
            commandParams.add("-ip");
            commandParams.add(consulContainer.getAdvertiseAddress());
            commandParams.add("consul://"+consulContainer.getNetworkAliases().get(0)+":"+consulContainer.getClientPort());
        }
        setCommandParts(commandParams.toArray(new String[0]));
    }

    @Override
    public RegistratorContainer withRegistratorExplicitOnly(boolean explicitOnly) {
        this.explicitOnly = explicitOnly;
        return this;
    }

    @Override
    public RegistratorContainer withForcedTags(Set<String> forcedTags) {
        this.forcedTags = forcedTags;
        return this;
    }

    @Override
    public boolean isInternalPorts() {
        return this.internalPorts;
    }

    @Override
    public boolean isRegistratorExplicitOnly() {
        return this.explicitOnly;
    }

    @Override
    public Set<String> getForcedTags() {
        return forcedTags;
    }
}
