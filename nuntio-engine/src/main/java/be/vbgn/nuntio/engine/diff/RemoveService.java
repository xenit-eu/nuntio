package be.vbgn.nuntio.engine.diff;

import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class RemoveService implements Diff {
    RegistryServiceIdentifier registryServiceIdentifier;
}
