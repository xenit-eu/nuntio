package be.vbgn.nuntio.registry.consul;

import be.vbgn.nuntio.api.identifier.ServiceIdentifier;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class ConsulServiceIdentifier implements RegistryServiceIdentifier {

    String serviceName;
    String serviceId;
    ServiceIdentifier serviceIdentifier;

    public static ConsulServiceIdentifier fromDescription(RegistryServiceDescription description) {
        return new ConsulServiceIdentifier(
                description.getName(),
                description.getServiceIdentifier().toHumanString(),
                description.getServiceIdentifier()
        );
    }
}
