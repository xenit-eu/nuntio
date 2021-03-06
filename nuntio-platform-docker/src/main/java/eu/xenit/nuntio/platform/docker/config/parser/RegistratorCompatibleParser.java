package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import eu.xenit.nuntio.platform.docker.DockerProperties.PortBindConfiguration;
import eu.xenit.nuntio.platform.docker.DockerProperties.RegistratorCompatibleProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser implementation that follows Gliderlabs' Registrator rules
 *
 * @see <a href="https://gliderlabs.github.io/registrator/latest/user/services/">Registrator User Guide - Service</a>
 */
@Slf4j
@AllArgsConstructor
public class RegistratorCompatibleParser implements ServiceConfigurationParser {

    @Setter(AccessLevel.PACKAGE)
    PortBindConfiguration portBindConfiguration;
    RegistratorCompatibleProperties registratorCompatibleProperties;

    @Value
    @ToString(onlyExplicitlyIncluded = true)
    private static class ContainerWithServiceBinding {
        ContainerMetadata containerMetadata;
        @ToString.Include
        ServiceBinding serviceBinding;

        @ToString.Include(name = "container", rank = 1)
        private String getContainerName() {
            return containerMetadata.getContainerName();
        }
    }

    @Override
    public Set<PlatformServiceConfiguration> toServiceConfigurations(ContainerMetadata containerMetadata) {
        Map<String, String> configuration = new HashMap<>();
        configuration.putAll(containerMetadata.getEnvironment());
        configuration.putAll(containerMetadata.getLabels());

        Set<ServiceBinding> serviceBindings = portBindConfiguration == PortBindConfiguration.INTERNAL?containerMetadata.getExposedPortBindings():containerMetadata.getPublishedPortBindings();
        log.debug("Container {} has relevant service bindings {}", containerMetadata.getContainerName(), serviceBindings);
        boolean hasMultipleBindings = serviceBindings.size() > 1;

        Set<PlatformServiceConfiguration> platformServiceConfigurations = new HashSet<>();
        for(ServiceBinding serviceBinding: serviceBindings) {
            ContainerWithServiceBinding containerWithServiceBinding = new ContainerWithServiceBinding(containerMetadata, serviceBinding);
            // If SERVICE_<port>_IGNORE or SERVICE_IGNORE is present and not empty, disregard this service
            boolean isIgnore = findValueWithFallback(ConfigKind.IGNORE, serviceBinding, configuration)
                    .filter(Predicate.not(String::isEmpty))
                    .isPresent();
            if(isIgnore) {
                log.debug("{} is ignored", containerWithServiceBinding);
                continue;
            }

            var platformServiceBuilder = PlatformServiceConfiguration.builder();
            platformServiceBuilder.serviceBinding(serviceBinding);

            String defaultServiceNameSuffix = hasMultipleBindings? "-"+serviceBinding.getPort().orElseThrow() :"";

            // Find service name: first look at SERVICE_<port>_NAME
            // then at SERVICE_NAME (and suffix it with the port number of there are multiple service bindings)
            // Finally fall back to image base name (also potentially suffixed with port number)
            Optional<String> maybeServiceName = findValue(ConfigKind.SERVICE_NAME, serviceBinding, configuration)
                    .or(() -> findValue(ConfigKind.SERVICE_NAME, ServiceBinding.ANY, configuration)
                            .map(defaultServiceName -> defaultServiceName + defaultServiceNameSuffix)
                    );
            // Registrator explicit mode: ignore containers unless SERVICE_NAME is set
            if(registratorCompatibleProperties.isExplicit() && maybeServiceName.isEmpty()) {
                log.debug("{} is ignored because no service name is configured", containerWithServiceBinding);
                continue;
            }
            String serviceName = maybeServiceName.orElseGet(() -> extractImageBaseName(containerMetadata.getImageName()) + defaultServiceNameSuffix);
            if(maybeServiceName.isEmpty()) {
                log.debug("Automatically set service name of {} to {} based on image name", containerWithServiceBinding, serviceName);
            } else {
                log.debug("Service name for {} is {}", containerWithServiceBinding, serviceName);
            }

            platformServiceBuilder.serviceName(serviceName);

            // Find service tags: first look at SERVICE_<port>_TAGS, then at SERVICE_TAGS
            findValueWithFallback(ConfigKind.TAGS, serviceBinding, configuration)
                    .ifPresent(tags -> {
                        platformServiceBuilder.serviceTags(Util.splitByComma(tags));
                    });

            Map<String, String> metadata = new HashMap<>();
            configuration.forEach((key, value) -> {
                if(!key.startsWith("SERVICE_")) {
                    return;
                }
                var parts = key.split("_", 3);
                if(parts.length == 3 && parts[1].matches("\\d+")) {
                    // SERVICE_<port>_<key>
                    if(parts[1].equals(serviceBinding.getPort().orElse(null))) {
                        metadata.put(parts[2].toLowerCase(Locale.ROOT), value);
                    }
                } else {
                    // SERVICE_<key>
                    // Only update when there is no key present yet, because SERVICE_<port>_<key> overrides this value
                    metadata.putIfAbsent(key.substring("SERVICE_".length()).toLowerCase(Locale.ROOT), value);
                }
            });

            metadata.remove("name");
            metadata.remove("id");
            metadata.remove("tags");

            platformServiceBuilder.serviceMetadata(metadata);

            platformServiceConfigurations.add(platformServiceBuilder.build());
        }

        return platformServiceConfigurations;
    }

    private String extractImageBaseName(String imageName) {
        var lastSlashIndex = imageName.lastIndexOf("/");
        var serviceNameBase = (lastSlashIndex == -1)
                ? imageName
                : imageName.substring(lastSlashIndex + 1);

        var firstColonIndex = serviceNameBase.indexOf(":");

        return (firstColonIndex == -1)
                ?serviceNameBase
                :serviceNameBase.substring(0, firstColonIndex);
    }

    private Optional<String> findValueWithFallback(ConfigKind configKind, ServiceBinding serviceBinding, Map<String, String> configuration) {
        return findValue(configKind, serviceBinding, configuration)
                .or(() -> findValue(configKind, ServiceBinding.ANY, configuration));
    }

    private Optional<String> findValue(ConfigKind configKind, ServiceBinding serviceBinding, Map<String, String> configuration) {
        String configKey = "SERVICE"
                +serviceBinding.getPort().map(port -> "_"+port).orElse("")
                +configKind.getEnvVarSuffix();
        if(configuration.getOrDefault(configKey, "").isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(configuration.get(configKey));
    }


    @AllArgsConstructor
    private enum ConfigKind {
        IGNORE("_IGNORE"),
        SERVICE_NAME("_NAME"),
        TAGS("_TAGS"),
        ;

        @Getter(value = AccessLevel.PRIVATE)
        private final String envVarSuffix;
    }

}
