package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import lombok.Value;

@Value
public class EqualService implements Diff {
    PlatformServiceDescription description;
    PlatformServiceConfiguration serviceConfiguration;
    RegistryServiceIdentifier registryServiceIdentifier;
}
