package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.checks.InvalidCheckException;
import eu.xenit.nuntio.api.checks.ServiceCheckFactory;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses a docker label for nuntio into its constituents.
 * <p>
 * The label is built up like {labelPrefix}/{serviceBinding?}/{labelKind}/{additional?}
 * <p>
 * Examples:
 * * nuntio/service
 * * nuntio/80/service
 * * nuntio/tcp:80/tags
 * * nuntio/udp:65/metadata/my-metadata
 * * nuntio/check/my-check/type
 */
@Slf4j
@AllArgsConstructor
public class NuntioLabelsParser implements ServiceConfigurationParser {

    private final String labelPrefix;
    private final ServiceCheckFactory serviceCheckFactory;

    private static final Pattern LABEL_WITHOUT_ADDITIONAL_PATTERN;
    private static final Pattern LABEL_WITH_ADDITIONAL_PATTERN;

    static {
        String labelKindNoAdditionalPattern =
                "(?<labelKind>" + createLabelPattern(Predicate.not(LabelKind::isAcceptsAdditional)) + ")";
        String labelKindWithAdditionalPattern =
                "(?<labelKind>" + createLabelPattern(LabelKind::isAcceptsAdditional) + ")";
        String serviceBindingPattern = "(?:(?<serviceBindingProtocol>udp|tcp):)?(?<serviceBindingPort>[0-9]+)";
        LABEL_WITHOUT_ADDITIONAL_PATTERN = Pattern.compile(
                "^(?:" + serviceBindingPattern + "/)?" + labelKindNoAdditionalPattern + "$");

        String additionalPattern = "/(?<labelAdditional>.*)";

        LABEL_WITH_ADDITIONAL_PATTERN = Pattern.compile(
                "^(?:" + serviceBindingPattern + "/)?" + labelKindWithAdditionalPattern + additionalPattern + "$");
    }

    private static String createLabelPattern(Predicate<? super LabelKind> filter) {
        return Arrays.stream(LabelKind.values()).filter(filter)
                .map(LabelKind::getIdentifier).collect(
                        Collectors.joining("|"));
    }

    @Override
    public Set<PlatformServiceConfiguration> toServiceConfigurations(ContainerMetadata containerMetadata) {
        var bindingsWithConfiguration = parseContainerMetadata(containerMetadata)
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getBinding()));

        Set<PlatformServiceConfiguration> serviceConfigurations = new HashSet<>();

        for (Entry<ServiceBinding, List<Entry<ParsedServiceConfiguration, String>>> bindingListEntry : bindingsWithConfiguration.entrySet()) {
            var services = findLabelOfType(bindingListEntry.getValue(), ConfigurationKind.SERVICE)
                    .map(Entry::getValue)
                    .map(Util::splitByComma);
            var tags = findLabelOfType(bindingListEntry.getValue(), ConfigurationKind.TAGS)
                    .map(Entry::getValue)
                    .map(Util::splitByComma)
                    .orElse(Collections.emptyList());
            var metadata = findLabelsOfType(bindingListEntry.getValue(), ConfigurationKind.METADATA)
                    .collect(Collectors.toMap(entry -> entry.getKey().getAdditional().get("metadataKey"), Entry::getValue));
            var checks = findLabelsOfType(bindingListEntry.getValue(), ConfigurationKind.CHECK)
                    .map(NuntioLabelsParser::createServiceCheckConfiguration)
                    .collect(Collectors.toMap(ServiceCheckConfiguration::key, Function.identity(), NuntioLabelsParser::mergeServiceCheckConfigurations))
                    .values()
                    .stream()
                    // Then only retain the ones with the required "type" option set
                    .filter(serviceCheckConfiguration -> serviceCheckConfiguration.getOptions().containsKey("type"))
                    .map(serviceCheckConfiguration -> {
                        Map<String, String> checkOptions = new HashMap<>(serviceCheckConfiguration.getOptions());
                        String type = checkOptions.remove("type");
                        try {
                            return serviceCheckFactory.createCheck(
                                    type,
                                    serviceCheckConfiguration.getName(),
                                    checkOptions
                            );
                        } catch (InvalidCheckException e) {
                            throw new InvalidMetadataException(e);
                        }
                    })
                    .collect(Collectors.toSet());

            if (services.isEmpty()) {
                log.warn("Binding {} is missing a service but has other configurations.",
                        bindingListEntry.getKey());
            } else if (services.get().isEmpty()) {
                log.warn("Binding {} has no service names.", bindingListEntry.getKey());
            }

            services
                    .filter(service -> !service.isEmpty())
                    .map(service -> PlatformServiceConfiguration.builder()
                            .serviceBinding(bindingListEntry.getKey())
                            .serviceNames(service)
                            .serviceTags(tags)
                            .serviceMetadata(metadata)
                            .checks(checks)
                            .build()
                    ).ifPresent(serviceConfigurations::add);
        }
        return serviceConfigurations;
    }

    private Map<ParsedServiceConfiguration, String> parseContainerMetadata(ContainerMetadata containerMetadata) {
        Map<ParsedServiceConfiguration, String> parsedLabels = new HashMap<>();
        for (Entry<String, String> labelEntry : containerMetadata.getLabels().entrySet()) {
            Optional<ParsedServiceConfiguration> parsedLabel = parseLabel(labelEntry.getKey());
            parsedLabel.ifPresent(label -> parsedLabels.put(label, labelEntry.getValue()));
        }
        return parsedLabels;
    }

    private Optional<ParsedServiceConfiguration> parseLabel(String label) {
        String labelPrefix = this.labelPrefix + "/";

        if (!label.startsWith(labelPrefix)) {
            return Optional.empty();
        }
        String withoutPrefix = label.substring(labelPrefix.length());

        Matcher labelWithoutAdditionalMatch = LABEL_WITHOUT_ADDITIONAL_PATTERN.matcher(withoutPrefix);
        if (labelWithoutAdditionalMatch.matches()) {
            return LabelKind.find(labelWithoutAdditionalMatch.group("labelKind"))
                    .map(labelKind -> new ParsedServiceConfiguration(labelKind.getConfigurationKind(),
                            createServiceBindingFromMatch(labelWithoutAdditionalMatch), null));
        }
        Matcher labelWithAdditionalMatch = LABEL_WITH_ADDITIONAL_PATTERN.matcher(withoutPrefix);
        if (labelWithAdditionalMatch.matches()) {
            return LabelKind.find(labelWithAdditionalMatch.group("labelKind"))
                    .flatMap(labelKind -> labelKind.matchAdditional(labelWithAdditionalMatch.group("labelAdditional"))
                            .map(additionalMatch ->
                                    new ParsedServiceConfiguration(labelKind.getConfigurationKind(),
                                            createServiceBindingFromMatch(labelWithAdditionalMatch),
                                            additionalMatch)
                            ));
        }
        return Optional.empty();
    }

    private static ServiceBinding createServiceBindingFromMatch(Matcher matcher) {
        String serviceBindingPort = matcher.group("serviceBindingPort");
        String serviceBindingProtocol = matcher.group("serviceBindingProtocol");

        if (serviceBindingPort == null) {
            return ServiceBinding.ANY;
        }
        if (serviceBindingProtocol == null) {
            return ServiceBinding.fromPort(serviceBindingPort);
        }

        return ServiceBinding.fromPortAndProtocol(serviceBindingPort, serviceBindingProtocol);
    }


    @AllArgsConstructor
    private enum LabelKind {
        SERVICE("service",  ConfigurationKind.SERVICE, null, null),
        TAGS("tags",  ConfigurationKind.TAGS, null, null),
        METADATA("metadata", ConfigurationKind.METADATA, Pattern.compile("^(?<metadataKey>.+)$"), new String[]{"metadataKey"}),
        CHECKS("check",  ConfigurationKind.CHECK,
                Pattern.compile("^(?<checkName>[a-z0-9\\-_]+)/(?<option>[a-z\\-_]+)$"), new String[] {"checkName", "option"}),
        ;

        @Getter(value = AccessLevel.PRIVATE)
        private final String identifier;

        @Getter(value = AccessLevel.PRIVATE)
        private final ConfigurationKind configurationKind;
        private final Pattern additionalPattern;
        private final String[] groups;

        private static Optional<LabelKind> find(String identifier) {
            for (LabelKind configurationKind : values()) {
                if (configurationKind.getIdentifier().equals(identifier)) {
                    return Optional.of(configurationKind);
                }
            }
            return Optional.empty();
        }

        public boolean isAcceptsAdditional() {
            return additionalPattern != null;
        }

        public Optional<Map<String, String>> matchAdditional(String additional) {
            return Optional.of(additionalPattern.matcher(additional))
                    .filter(Matcher::matches)
                    .map(matcher -> Arrays.stream(groups)
                            .collect(Collectors.toMap(Function.identity(), matcher::group))
                    );
        }
    }

    private static Stream<Entry<ParsedServiceConfiguration, String>> findLabelsOfType(List<Entry<ParsedServiceConfiguration, String>> labels,
            ConfigurationKind type) {
        return labels.stream().filter(e -> e.getKey().getConfigurationKind() == type);
    }

    private static Optional<Entry<ParsedServiceConfiguration, String>> findLabelOfType(List<Entry<ParsedServiceConfiguration, String>> labels,
            ConfigurationKind type) {
        return findLabelsOfType(labels, type).findFirst();
    }

    @Value
    private static class ParsedServiceConfiguration {
        ConfigurationKind configurationKind;
        ServiceBinding binding;
        Map<String, String> additional;
    }

    private enum ConfigurationKind {
        SERVICE,
        TAGS,
        METADATA,
        CHECK;
    }

    @Value
    private static class ServiceCheckConfiguration {
        String name;
        ServiceBinding serviceBinding;
        Map<String, String> options;

        ServiceCheckConfigurationKey key() {
            return new ServiceCheckConfigurationKey(name, serviceBinding);
        }
    }

    @Value
    private static class ServiceCheckConfigurationKey {
        String name;
        ServiceBinding serviceBinding;
    }

    private static ServiceCheckConfiguration createServiceCheckConfiguration(Entry<ParsedServiceConfiguration, String> entry) {
        return new ServiceCheckConfiguration(
                entry.getKey().getAdditional().get("checkName"),
                entry.getKey().getBinding(),
                Collections.singletonMap(entry.getKey().getAdditional().get("option"), entry.getValue())
        );
    }

    private static ServiceCheckConfiguration mergeServiceCheckConfigurations(ServiceCheckConfiguration c1, ServiceCheckConfiguration c2) {
        if(!Objects.equals(c1.key(), c2.key())) {
            throw new IllegalArgumentException("Merging 2 unrelated check configurations is not supported.");
        }

        Map<String, String> mergedMap = new HashMap<>(c1.getOptions().size()+c2.getOptions().size());
        mergedMap.putAll(c1.getOptions());
        mergedMap.putAll(c2.getOptions());

        return new ServiceCheckConfiguration(c1.getName(), c1.getServiceBinding(), mergedMap);
    }

}
