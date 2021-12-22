package eu.xenit.nuntio.integtest.containers;

import java.util.Set;
import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

public interface RegistrationContainer<T extends RegistrationContainer<T>> extends Container<T>, Startable {
    T withDindContainer(DindContainer dindContainer);
    T withConsulContainer(ConsulContainer consulContainer);

    T withInternalPorts(boolean internalPorts);
    T withRegistratorExplicitOnly(boolean explicitOnly);
    T withForcedTags(Set<String> forcedTags);

    boolean isInternalPorts();
    boolean isRegistratorExplicitOnly();
    Set<String> getForcedTags();
}
