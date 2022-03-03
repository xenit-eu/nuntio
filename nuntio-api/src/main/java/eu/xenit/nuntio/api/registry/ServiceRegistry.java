package eu.xenit.nuntio.api.registry;

import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ServiceRegistry {

    Set<? extends RegistryServiceIdentifier> findServices();

    default Set<RegistryServiceIdentifier> findServices(PlatformIdentifier sharedIdentifier) {
        return findServices()
                .stream()
                .filter(serviceIdentifier -> Objects.equals(serviceIdentifier.getPlatformIdentifier(), sharedIdentifier))
                .collect(Collectors.toSet());
    }

    default Map<? extends RegistryServiceIdentifier, RegistryServiceDescription> findServiceDescriptions(PlatformIdentifier sharedIdentifier) {
        return findServices(sharedIdentifier)
                .stream()
                .collect(Collectors.toMap(Function.identity(), sid -> findServiceDescription(sid).orElseThrow()));
    }

    Optional<RegistryServiceDescription> findServiceDescription(RegistryServiceIdentifier serviceIdentifier);

    default Map<? extends RegistryServiceIdentifier, RegistryServiceDescription> findServiceDescriptions() {
        return findServices()
                .stream()
                .collect(Collectors.toMap(Function.identity(), sid -> findServiceDescription(sid).orElseThrow()));
    }

    RegistryServiceIdentifier registerService(RegistryServiceDescription description);

    void unregisterService(RegistryServiceIdentifier serviceIdentifier);

    void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType);

    void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message);

}
