package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class RemoveService implements Diff {
    RegistryServiceIdentifier registryServiceIdentifier;
}
