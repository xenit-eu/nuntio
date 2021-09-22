package be.vbgn.nuntio.platform.docker.config;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.stream.Stream;

public interface ServiceConfigurationModifier {

    Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse);
}
