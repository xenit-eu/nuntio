package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface ServiceRegistry {

    Set<? extends RegistryServiceIdentifier> findServices();

    default Set<RegistryServiceIdentifier> findAll(SharedIdentifier sharedIdentifier) {
        return findServices()
                .stream()
                .filter(serviceIdentifier -> Objects.equals(serviceIdentifier.getSharedIdentifier(), sharedIdentifier))
                .collect(Collectors.toSet());
    }

    default Set<RegistryServiceIdentifier> findAll(PlatformServiceDescription platformServiceDescription) {
        return findAll(platformServiceDescription.getIdentifier().getSharedIdentifier());
    }

    RegistryServiceIdentifier registerService(RegistryServiceDescription description);

    void unregisterService(RegistryServiceIdentifier serviceIdentifier);

    void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message);

}
