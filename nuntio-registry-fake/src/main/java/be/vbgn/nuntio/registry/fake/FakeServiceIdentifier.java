package be.vbgn.nuntio.registry.fake;

import be.vbgn.nuntio.api.identifier.PlatformIdentifier;
import be.vbgn.nuntio.api.identifier.ServiceIdentifier;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class FakeServiceIdentifier implements RegistryServiceIdentifier {

    private ServiceIdentifier serviceIdentifier;

    public static FakeServiceIdentifier create(RegistryServiceDescription registryServiceDescription) {
        return new FakeServiceIdentifier(registryServiceDescription.getServiceIdentifier());
    }
}
