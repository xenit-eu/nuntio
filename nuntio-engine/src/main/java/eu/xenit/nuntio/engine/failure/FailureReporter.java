package eu.xenit.nuntio.engine.failure;

import eu.xenit.nuntio.api.registry.errors.RegistryOperationException;

public interface FailureReporter {
    void reportRegistryFailure(RegistryOperationException exception);
}
