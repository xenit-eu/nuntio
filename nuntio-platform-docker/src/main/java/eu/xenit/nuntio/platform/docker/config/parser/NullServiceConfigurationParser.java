package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import java.util.Collections;
import java.util.Set;

public class NullServiceConfigurationParser implements ServiceConfigurationParser{

    @Override
    public Set<PlatformServiceConfiguration> toServiceConfigurations(ContainerMetadata containerMetadata) {
        return Collections.emptySet();
    }
}
