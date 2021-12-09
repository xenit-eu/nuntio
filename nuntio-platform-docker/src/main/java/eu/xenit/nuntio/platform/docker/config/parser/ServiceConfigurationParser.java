package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import java.util.Set;

public interface ServiceConfigurationParser {
    Set<PlatformServiceConfiguration> toServiceConfigurations(ContainerMetadata containerMetadata);
}
