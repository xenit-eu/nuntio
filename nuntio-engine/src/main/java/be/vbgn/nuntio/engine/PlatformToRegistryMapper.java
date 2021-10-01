package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlatformToRegistryMapper {

    private static final String NUNTIO_RESERVED_PREFIX = "nuntio-";

    public Set<RegistryServiceDescription> createServices(PlatformServiceDescription serviceDescription) {
        return serviceDescription.getServiceConfigurations()
                .stream()
                .flatMap(configuration -> configuration.getServiceNames().stream()
                        .map(serviceName -> new RegistryServiceDescription(
                                serviceName,
                                serviceDescription.getIdentifier().getSharedIdentifier(),
                                configuration.getServiceBinding().getIp(),
                                configuration.getServiceBinding().getPort().orElseThrow(),
                                configuration.getServiceTags(),
                                createMetadata(configuration)
                        )))
                .collect(Collectors.toSet());
    }

    private Map<String, String> createMetadata(PlatformServiceConfiguration configuration) {
        final var serviceMetadata = configuration.getServiceMetadata();
        final var internalMetadata = configuration.getInternalMetadata();
        Map<String, String> allMetadata = new HashMap<>(serviceMetadata.size() + internalMetadata.size());
        serviceMetadata.forEach((key, value) -> {
            if (!key.startsWith(NUNTIO_RESERVED_PREFIX)) {
                allMetadata.put(key, value);
            } else {
                log.warn(
                        "Platform configuration {} specified invalid metadata. Metadata key {} is reserved.",
                        configuration, key);
            }
        });
        internalMetadata.forEach((key, value) -> {
            allMetadata.put(NUNTIO_RESERVED_PREFIX + key, value);
        });
        return allMetadata;
    }

}
