package be.vbgn.nuntio.registry.fake;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class FakeServiceIdentifier implements RegistryServiceIdentifier {

    private SharedIdentifier sharedIdentifier;
    private String port;

    public static FakeServiceIdentifier create(SharedIdentifier sharedIdentifier, RegistryServiceDescription registryServiceDescription) {
        return new FakeServiceIdentifier(sharedIdentifier, registryServiceDescription.getPort());
    }

    @Override
    public SharedIdentifier getSharedIdentifier() {
        return sharedIdentifier;
    }
}
