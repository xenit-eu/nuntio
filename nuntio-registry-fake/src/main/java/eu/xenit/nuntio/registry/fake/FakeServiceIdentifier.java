package eu.xenit.nuntio.registry.fake;

import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class FakeServiceIdentifier implements RegistryServiceIdentifier {

    private ServiceIdentifier serviceIdentifier;

    public static FakeServiceIdentifier create(RegistryServiceDescription registryServiceDescription) {
        return new FakeServiceIdentifier(registryServiceDescription.getServiceIdentifier());
    }
}
