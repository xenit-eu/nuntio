package eu.xenit.nuntio.api.registry.errors;

import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Getter;

@Getter
public class ServiceDeregistrationException extends RegistryOperationException {
    private final RegistryServiceIdentifier serviceIdentifier;

    public ServiceDeregistrationException(RegistryServiceIdentifier registryServiceIdentifier, String message) {
        super(message);
        this.serviceIdentifier = registryServiceIdentifier;
    }

    public ServiceDeregistrationException(RegistryServiceIdentifier registryServiceIdentifier,  String message, Throwable cause) {
        super(message, cause);
        this.serviceIdentifier = registryServiceIdentifier;
    }
}
