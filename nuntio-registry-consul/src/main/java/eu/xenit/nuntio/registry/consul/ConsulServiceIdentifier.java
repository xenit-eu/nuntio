package eu.xenit.nuntio.registry.consul;

import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
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
