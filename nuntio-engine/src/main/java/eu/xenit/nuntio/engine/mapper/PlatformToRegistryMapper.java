package eu.xenit.nuntio.engine.mapper;

import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlatformToRegistryMapper {
    private static final String NUNTIO_RESERVED_PREFIX = "nuntio-";

    public Stream<RegistryServiceDescription> registryServiceDescriptionsFor(
            PlatformServiceDescription platformServiceDescription,
            PlatformServiceConfiguration platformServiceConfiguration) {
        return platformServiceConfiguration.getServiceNames().stream()
                .peek(serviceName -> log.debug("Creating registry service {} for platform {}", serviceName,
                        platformServiceConfiguration))
                .map(serviceName -> RegistryServiceDescription.builder()
                        .name(serviceName)
                        .platformIdentifier(platformServiceDescription.getIdentifier().getPlatformIdentifier())
                        .serviceIdentifier(ServiceIdentifier.of(platformServiceDescription.getIdentifier()
                                .getPlatformIdentifier(), platformServiceConfiguration.getServiceBinding()))
                        .address(platformServiceConfiguration.getServiceBinding().getIp())
                        .port(platformServiceConfiguration.getServiceBinding().getPort().orElseThrow())
                        .tags(platformServiceConfiguration.getServiceTags())
                        .metadata(createMetadata(platformServiceConfiguration))
                        .build()
                );
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
