package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import java.util.Set;

public interface ServiceRegistry {

    Set<RegistryServiceIdentifier> findServices();

    Set<RegistryServiceIdentifier> findAll(SharedIdentifier sharedIdentifier);

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
