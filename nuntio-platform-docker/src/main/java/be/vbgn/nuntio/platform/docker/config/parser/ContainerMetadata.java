package be.vbgn.nuntio.platform.docker.config.parser;

import java.util.Map;

public interface ContainerMetadata {

    Map<String, String> getEnvironment();

    Map<String, String> getLabels();
}
