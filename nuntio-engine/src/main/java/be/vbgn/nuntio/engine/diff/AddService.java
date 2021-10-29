package be.vbgn.nuntio.engine.diff;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import lombok.Value;

@Value
public class AddService implements Diff{
    PlatformServiceDescription description;
    PlatformServiceConfiguration serviceConfiguration;
}
