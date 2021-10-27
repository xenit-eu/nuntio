package be.vbgn.nuntio.platform.docker.config.parser;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import java.util.Collections;
import java.util.Set;

public class NullServiceConfigurationParser implements ServiceConfigurationParser{

    @Override
    public Set<PlatformServiceConfiguration> toServiceConfigurations(ContainerMetadata containerMetadata) {
        return Collections.emptySet();
    }
}
