package eu.xenit.nuntio.api.registry.errors;

import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Getter;

@Getter
public class ServiceOperationException extends RegistryOperationException {
    private final RegistryServiceIdentifier serviceIdentifier;

    public ServiceOperationException(RegistryServiceIdentifier registryServiceIdentifier, String message) {
        super(message);
        this.serviceIdentifier = registryServiceIdentifier;
    }

    public ServiceOperationException(RegistryServiceIdentifier registryServiceIdentifier,  String message, Throwable cause) {
        super(message, cause);
        this.serviceIdentifier = registryServiceIdentifier;
    }
}
