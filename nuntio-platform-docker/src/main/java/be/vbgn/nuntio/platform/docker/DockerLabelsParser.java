package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.platform.ServiceBinding;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
@Component
public class DockerLabelsParser {


    private final String labelPrefix;

    private static final Pattern LABEL_WITHOUT_ADDITIONAL_PATTERN;
    private static final Pattern LABEL_WITH_ADDITIONAL_PATTERN;

    static {
        String labelKindNoAdditionalPattern =
                "(?<labelKind>" + createLabelPattern(Predicate.not(Label::isAcceptsAdditional)) + ")";
        String labelKindWithAdditionalPattern =
                "(?<labelKind>" + createLabelPattern(Label::isAcceptsAdditional) + ")";
        String serviceBindingPattern = "(?:(?<serviceBindingProtocol>udp|tcp):)?(?<serviceBindingPort>[0-9]+)";
        LABEL_WITHOUT_ADDITIONAL_PATTERN = Pattern.compile(
                "^(?:" + serviceBindingPattern + "/)?" + labelKindNoAdditionalPattern + "$");

        String additionalPattern = "/(?<labelAdditional>.*)";

        LABEL_WITH_ADDITIONAL_PATTERN = Pattern.compile(
                "^(?:" + serviceBindingPattern + "/)?" + labelKindWithAdditionalPattern + additionalPattern + "$");
    }

    private static String createLabelPattern(Predicate<? super Label> filter) {
        return Arrays.stream(Label.values()).filter(filter)
                .map(Label::getLabelKind).collect(
                        Collectors.joining("|"));
    }

    public DockerLabelsParser(@Value("${nuntio.docker.label-prefix}") String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    @AllArgsConstructor
    public enum Label {
        SERVICE("service", false),
        TAGS("tags", false),
        METADATA("metadata", true),
        ;

        @Getter(value = AccessLevel.PRIVATE)
        private final String labelKind;

        @Getter(value = AccessLevel.PRIVATE)
        private final boolean acceptsAdditional;

        private static Optional<Label> find(String labelKind) {
            for (Label label : values()) {
                if (label.getLabelKind().equals(labelKind)) {
                    return Optional.of(label);
                }
            }
            return Optional.empty();
        }
    }

    @lombok.Value
    public static class ParsedLabel {

        Label labelKind;
        ServiceBinding binding;
        String additional;
    }

    public Map<ParsedLabel, String> parseLabels(Map<String, String> labels) {
        Map<ParsedLabel, String> parsedLabels = new HashMap<>();
        for (Entry<String, String> labelEntry : labels.entrySet()) {
            Optional<ParsedLabel> parsedLabel = parseLabel(labelEntry.getKey());
            parsedLabel.ifPresent(label -> parsedLabels.put(label, labelEntry.getValue()));
        }
        return parsedLabels;
    }

    private Optional<ParsedLabel> parseLabel(String label) {
        String labelPrefix = this.labelPrefix + "/";

        if (!label.startsWith(labelPrefix)) {
            return Optional.empty();
        }
        String withoutPrefix = label.substring(labelPrefix.length());

        Matcher labelWithoutAdditionalMatch = LABEL_WITHOUT_ADDITIONAL_PATTERN.matcher(withoutPrefix);
        if (labelWithoutAdditionalMatch.matches()) {
            return Label.find(labelWithoutAdditionalMatch.group("labelKind"))
                    .map(labelKind -> new ParsedLabel(labelKind,
                            createServiceBindingFromMatch(labelWithoutAdditionalMatch), null));
        }
        Matcher labelWithAdditionalMatch = LABEL_WITH_ADDITIONAL_PATTERN.matcher(withoutPrefix);
        if (labelWithAdditionalMatch.matches()) {
            return Label.find(labelWithAdditionalMatch.group("labelKind"))
                    .map(labelKind -> new ParsedLabel(labelKind,
                            createServiceBindingFromMatch(labelWithAdditionalMatch),
                            labelWithAdditionalMatch.group("labelAdditional")));
        }
        return Optional.empty();
    }

    private static ServiceBinding createServiceBindingFromMatch(Matcher matcher) {
        String serviceBindingPort = matcher.group("serviceBindingPort");
        String serviceBindingProtocol = matcher.group("serviceBindingProtocol");

        if (serviceBindingPort == null || serviceBindingPort.isEmpty()) {
            return ServiceBinding.ANY;
        }
        if (serviceBindingProtocol == null || serviceBindingProtocol.isEmpty()) {
            return ServiceBinding.fromPort(serviceBindingPort);
        }

        return ServiceBinding.fromPortAndProtocol(serviceBindingPort, serviceBindingProtocol);
    }


}
