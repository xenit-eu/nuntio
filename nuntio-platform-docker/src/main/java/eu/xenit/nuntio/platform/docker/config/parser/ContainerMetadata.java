package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Map;
import java.util.Set;

public interface ContainerMetadata {

    String getContainerName();

    String getImageName();

    Set<ServiceBinding> getExposedPortBindings();
    Set<ServiceBinding> getPublishedPortBindings();

    Map<String, String> getEnvironment();

    Map<String, String> getLabels();
}
