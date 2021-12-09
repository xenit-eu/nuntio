package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import lombok.Value;

@Value
public class AddService implements Diff{
    PlatformServiceDescription description;
    PlatformServiceConfiguration serviceConfiguration;
}
