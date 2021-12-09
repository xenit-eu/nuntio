package eu.xenit.nuntio.platform.docker.config.modifier;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.stream.Stream;

public interface ServiceConfigurationModifier {

    Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse);
}
