package be.vbgn.nuntio.registry.fake;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class FakeServiceIdentifier implements RegistryServiceIdentifier {

    private SharedIdentifier sharedIdentifier;

    public static FakeServiceIdentifier create(SharedIdentifier sharedIdentifier) {
        return new FakeServiceIdentifier(sharedIdentifier);
    }

    @Override
    public SharedIdentifier getSharedIdentifier() {
        return sharedIdentifier;
    }
}
