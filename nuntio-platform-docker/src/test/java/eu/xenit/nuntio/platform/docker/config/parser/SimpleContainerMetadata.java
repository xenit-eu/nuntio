package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class SimpleContainerMetadata implements ContainerMetadata{
    String imageName;
    @Singular
    Set<ServiceBinding> internalPortBindings;
    @Singular("environment")
    Map<String, String> environment;
    @Singular
    Map<String, String> labels;
}
