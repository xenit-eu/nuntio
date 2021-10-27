package be.vbgn.nuntio.platform.docker.config.parser;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import java.util.Set;

public interface ServiceConfigurationParser {
    Set<PlatformServiceConfiguration> toServiceConfigurations(ContainerMetadata containerMetadata);
}
