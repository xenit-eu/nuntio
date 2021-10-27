package be.vbgn.nuntio.registry.fake;

import be.vbgn.nuntio.api.identifier.PlatformIdentifier;
import be.vbgn.nuntio.api.identifier.ServiceIdentifier;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class FakeServiceIdentifier implements RegistryServiceIdentifier {

    private PlatformIdentifier platformIdentifier;
    private ServiceIdentifier serviceIdentifier;
    private String port;

    public static FakeServiceIdentifier create( RegistryServiceDescription registryServiceDescription) {
        return new FakeServiceIdentifier(registryServiceDescription.getPlatformIdentifier(), registryServiceDescription.getServiceIdentifier(), registryServiceDescription.getPort());
    }
}
