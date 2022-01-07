package eu.xenit.nuntio.api.registry;

import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import eu.xenit.nuntio.api.registry.errors.ServiceDeregistrationException;
import eu.xenit.nuntio.api.registry.errors.ServiceOperationException;
import eu.xenit.nuntio.api.registry.errors.ServiceRegistrationException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface ServiceRegistry {

    Set<? extends RegistryServiceIdentifier> findServices();

    default Set<RegistryServiceIdentifier> findAll(PlatformIdentifier sharedIdentifier) {
        return findServices()
                .stream()
                .filter(serviceIdentifier -> Objects.equals(serviceIdentifier.getPlatformIdentifier(), sharedIdentifier))
                .collect(Collectors.toSet());
    }

    RegistryServiceIdentifier registerService(RegistryServiceDescription description) throws ServiceRegistrationException;

    void unregisterService(RegistryServiceIdentifier serviceIdentifier) throws ServiceDeregistrationException;

    void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) throws ServiceOperationException;

    void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) throws ServiceOperationException;

    void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message) throws ServiceOperationException;

}
