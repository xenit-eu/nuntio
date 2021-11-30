package be.vbgn.nuntio.integtest.containers;

import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

public interface RegistrationContainer<T extends RegistrationContainer<T>> extends Container<T>, Startable {
    T withDindContainer(DindContainer dindContainer);
    T withConsulContainer(ConsulContainer consulContainer);

    T withInternalPorts(boolean internalPorts);
    T withRegistratorExplicitOnly(boolean explicitOnly);

    boolean isInternalPorts();
    boolean isRegistratorExplicitOnly();
}
