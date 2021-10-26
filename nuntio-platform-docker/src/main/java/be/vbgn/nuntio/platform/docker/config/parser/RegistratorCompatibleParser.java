package be.vbgn.nuntio.platform.docker.config.parser;

import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.config.parser.ParsedServiceConfiguration.ConfigurationKind;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

/**
 * Parser implementation that follows Gliderlabs' Registrator rules
 *
 * @see <a href="https://gliderlabs.github.io/registrator/latest/user/services/">Registrator User Guide - Service
 */
@Slf4j
public class RegistratorCompatibleParser implements ServiceConfigurationParser {

    @Override
    public Map<ParsedServiceConfiguration, String> parseContainerMetadata(ContainerMetadata containerMetadata) {
        Map<String, String> configuration = new HashMap<>();
        configuration.putAll(containerMetadata.getEnvironment());
        configuration.putAll(containerMetadata.getLabels());

        boolean hasMultipleBindings = containerMetadata.getInternalPortBindings().size() > 1;

        Map<ParsedServiceConfiguration, String> parsedConfigs = new HashMap<>();
        for(ServiceBinding serviceBinding: containerMetadata.getInternalPortBindings()) {
            // If SERVICE_<port>_IGNORE or SERVICE_IGNORE is present, disregard this service
            boolean isIgnore = findValueWithFallback(ConfigKind.IGNORE, serviceBinding, configuration).isPresent();
            if(isIgnore) {
                log.debug("Service binding {} is ignored", serviceBinding);
                continue;
            }

            String defaultServiceNameSuffix = hasMultipleBindings? "-"+serviceBinding.getPort() :"";

            // Find service name: first look at SERVICE_<port>_NAME
            // then at SERVICE_NAME (and suffix it with the port number of there are multiple service bindings)
            // Finally fall back to image base name (also potentially suffixed with port number)
            String serviceName = findValue(ConfigKind.SERVICE_NAME, serviceBinding, configuration)
                    .or(() -> findValue(ConfigKind.SERVICE_NAME, ServiceBinding.ANY, configuration)
                            .map(defaultServiceName -> {
                                return defaultServiceName + defaultServiceNameSuffix;
                            })
                    )
                    .orElseGet(() -> extractImageBaseName(containerMetadata.getImageName()) + defaultServiceNameSuffix);

            parsedConfigs.put(new ParsedServiceConfiguration(ConfigurationKind.SERVICE, serviceBinding, null), serviceName);

            // Find service tags: first look at SERVICE_<port>_TAGS, then at SERVICE_TAGS
            findValueWithFallback(ConfigKind.TAGS, serviceBinding, configuration)
                    .ifPresent(tags -> {
                        parsedConfigs.put(new ParsedServiceConfiguration(ConfigurationKind.TAGS, serviceBinding, null), tags);
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
                        metadata.put(parts[2], value);
                    }
                } else {
                    // SERVICE_<key>
                    // Only update when there is no key present yet, because SERVICE_<port>_<key> overrides this value
                    metadata.putIfAbsent(key.substring("SERVICE_".length()), value);
                }
            });

            metadata.remove("name");
            metadata.remove("id");
            metadata.remove("tags");
        }

        return parsedConfigs;
    }

    private String extractImageBaseName(String imageName) {
        var lastSlashIndex = imageName.lastIndexOf("/");
        var serviceNameBase = (lastSlashIndex == -1)
                ? imageName
                : imageName.substring(lastSlashIndex + 1);

        var firstColonIndex = serviceNameBase.indexOf(":");

        return (firstColonIndex == -1)?serviceNameBase:serviceNameBase.substring(0, firstColonIndex);
    }

    private Optional<String> findValueWithFallback(ConfigKind configKind, ServiceBinding serviceBinding, Map<String, String> configuration) {
        return findValue(configKind, serviceBinding, configuration)
                .or(() -> findValue(configKind, ServiceBinding.ANY, configuration));
    }

    private Optional<String> findValue(ConfigKind configKind, ServiceBinding serviceBinding, Map<String, String> configuration) {
        String configKey = "SERVICE"
                +serviceBinding.getPort().map(port -> "_"+port).orElse("")
                +configKind.envVarSuffix;
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
