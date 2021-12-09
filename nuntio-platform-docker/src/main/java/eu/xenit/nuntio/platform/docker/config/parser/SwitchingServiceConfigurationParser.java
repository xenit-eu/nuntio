package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SwitchingServiceConfigurationParser implements ServiceConfigurationParser {
    private List<ServiceConfigurationParser> parsers;

    @Override
    public Set<PlatformServiceConfiguration> toServiceConfigurations(ContainerMetadata containerMetadata) {
        for (ServiceConfigurationParser parser : parsers) {
            var serviceConfigurations = parser.toServiceConfigurations(containerMetadata);
            if(!serviceConfigurations.isEmpty()) {
                return serviceConfigurations.stream()
                        .map(serviceConfiguration -> serviceConfiguration.toBuilder()
                                .internalMetadata("configuration-parser", parser.getClass().getCanonicalName())
                                .build())
                        .collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }
}
