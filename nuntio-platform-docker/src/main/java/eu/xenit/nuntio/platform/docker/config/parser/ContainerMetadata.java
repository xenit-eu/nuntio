package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Map;
import java.util.Set;

public interface ContainerMetadata {

    String getImageName();

    Set<ServiceBinding> getInternalPortBindings();

    Map<String, String> getEnvironment();

    Map<String, String> getLabels();
}
