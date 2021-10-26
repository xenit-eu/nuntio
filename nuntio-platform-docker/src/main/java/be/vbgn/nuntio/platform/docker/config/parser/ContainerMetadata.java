package be.vbgn.nuntio.platform.docker.config.parser;

import be.vbgn.nuntio.api.platform.ServiceBinding;
import java.util.Map;
import java.util.Set;

public interface ContainerMetadata {

    String getImageName();

    Set<ServiceBinding> getInternalPortBindings();

    Map<String, String> getEnvironment();

    Map<String, String> getLabels();
}
