package eu.xenit.nuntio.api.registry.errors;


import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import lombok.Getter;

@Getter
public class ServiceRegistrationException extends RegistryOperationException {
    private final RegistryServiceDescription registryServiceDescription;

    public ServiceRegistrationException(RegistryServiceDescription registryServiceDescription, String message) {
        super(message);
        this.registryServiceDescription = registryServiceDescription;
    }

    public ServiceRegistrationException(RegistryServiceDescription registryServiceDescription, String message, Throwable cause) {
        super(message, cause);
        this.registryServiceDescription = registryServiceDescription;
    }
}
