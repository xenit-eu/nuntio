package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public interface ServiceRegistry {

    Set<RegistryServiceIdentifier> findServices();

    Set<RegistryServiceIdentifier> findAll(SharedIdentifier sharedIdentifier);

    default Set<RegistryServiceIdentifier> findAll(PlatformServiceDescription platformServiceDescription) {
        return platformServiceDescription.getServiceConfigurations()
                .stream()
                .map(configuration -> findAll(configuration.getSharedIdentifier()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    RegistryServiceIdentifier registerService(RegistryServiceDescription description);

    void unregisterService(RegistryServiceIdentifier serviceIdentifier);

    void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message);

}
