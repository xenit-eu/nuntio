package eu.xenit.nuntio.engine.failure;

import eu.xenit.nuntio.api.registry.errors.RegistryOperationException;

public class NullFailureReporter implements FailureReporter {

    @Override
    public void reportRegistryFailure(RegistryOperationException exception) {
        // NO-OP
    }
}
