package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.identifier.PlatformIdentifier;
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

    RegistryServiceIdentifier registerService(RegistryServiceDescription description);

    void unregisterService(RegistryServiceIdentifier serviceIdentifier);

    void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message);

}
