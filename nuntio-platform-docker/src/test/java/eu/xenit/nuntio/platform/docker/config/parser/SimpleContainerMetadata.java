package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Collections;
import java.util.HashSet;
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
    Set<ServiceBinding> exposedPortBindings;
    @Singular
    Set<ServiceBinding> publishedPortBindings;
    @Singular("environment")
    Map<String, String> environment;
    @Singular
    Map<String, String> labels;

    public Set<ServiceBinding> getExposedPortBindings() {
        var set = new HashSet<ServiceBinding>();
        set.addAll(exposedPortBindings);
        set.addAll(publishedPortBindings);
        return Collections.unmodifiableSet(set);
    }
}
