package be.vbgn.nuntio.registry.consul;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class ConsulServiceIdentifier implements RegistryServiceIdentifier {

    String serviceName;
    String serviceId;
    SharedIdentifier sharedIdentifier;

    public static ConsulServiceIdentifier fromDescription(RegistryServiceDescription description) {
        return new ConsulServiceIdentifier(
                description.getName(),
                description.getSharedIdentifier().toHumanString(),
                description.getSharedIdentifier()
        );
    }
}
