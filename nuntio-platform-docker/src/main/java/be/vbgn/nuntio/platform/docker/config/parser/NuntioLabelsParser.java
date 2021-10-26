package be.vbgn.nuntio.platform.docker.config.parser;

import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.config.parser.ParsedServiceConfiguration.ConfigurationKind;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
 */
public class NuntioLabelsParser implements ServiceConfigurationParser {

    private final String labelPrefix;

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

    public NuntioLabelsParser(String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    @Override
    public Map<ParsedServiceConfiguration, String> parseContainerMetadata(ContainerMetadata containerMetadata) {
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
                    .map(labelKind -> new ParsedServiceConfiguration(labelKind.getConfigurationKind(),
                            createServiceBindingFromMatch(labelWithAdditionalMatch),
                            labelWithAdditionalMatch.group("labelAdditional")));
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
        SERVICE("service", false, ConfigurationKind.SERVICE),
        TAGS("tags", false, ConfigurationKind.TAGS),
        METADATA("metadata", true, ConfigurationKind.METADATA),
        ;

        @Getter(value = AccessLevel.PRIVATE)
        private final String identifier;

        @Getter(value = AccessLevel.PRIVATE)
        private final boolean acceptsAdditional;

        @Getter(value = AccessLevel.PRIVATE)
        private final ConfigurationKind configurationKind;

        static Optional<LabelKind> find(String identifier) {
            for (LabelKind configurationKind : values()) {
                if (configurationKind.getIdentifier().equals(identifier)) {
                    return Optional.of(configurationKind);
                }
            }
            return Optional.empty();
        }
    }


}
