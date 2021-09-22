package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ServiceRegistry {

    Set<RegistryServiceIdentifier> findServices();

    Optional<RegistryServiceIdentifier> find(SharedIdentifier sharedIdentifier);

    default Set<RegistryServiceIdentifier> findAll(PlatformServiceDescription platformServiceDescription) {
        return platformServiceDescription.getServiceConfigurations()
                .stream()
                .map(configuration -> find(configuration.getSharedIdentifier()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    RegistryServiceIdentifier registerService(RegistryServiceDescription description);

    void unregisterService(RegistryServiceIdentifier serviceIdentifier);

    void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message);

}
