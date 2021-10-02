package be.vbgn.nuntio.platform.docker.config.parser;

import java.util.Map;

public interface ServiceConfigurationParser {
    Map<ParsedServiceConfiguration, String> parseContainerMetadata(ContainerMetadata containerMetadata);
}
