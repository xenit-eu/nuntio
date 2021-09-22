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

@Component
public class DockerLabelsParser {


    private final String labelPrefix;

    private static final Pattern LABEL_WITHOUT_ADDITIONAL_PATTERN;
    private static final Pattern LABEL_WITH_ADDITIONAL_PATTERN;

    static {
        String labelKindNoAdditionalPattern =
                "(?<labelKind>" + Arrays.stream(Label.values()).filter(Predicate.not(Label::isAcceptsAdditional))
                        .map(Label::getLabelKind).collect(
                                Collectors.joining("|")) + ")";
        String labelKindWithAdditionalPattern =
                "(?<labelKind>" + Arrays.stream(Label.values()).filter(Label::isAcceptsAdditional)
                        .map(Label::getLabelKind).collect(
                                Collectors.joining("|")) + ")";
        String serviceBindingPattern = "(?<serviceBindingPort>[0-9]+)(?:/(?<serviceBindingProtocol>udp|tcp))?";
        LABEL_WITHOUT_ADDITIONAL_PATTERN = Pattern.compile(
                "^" + labelKindNoAdditionalPattern + "(?:/" + serviceBindingPattern + ")?$");

        String additionalPattern = "/(?<labelAdditional>.*)";

        LABEL_WITH_ADDITIONAL_PATTERN = Pattern.compile(
                "^" + labelKindWithAdditionalPattern + "(?:/" + serviceBindingPattern + ")?" + additionalPattern + "$");
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

    /**
     * Label is built up like {labelPrefix}/{labelKind}(/{serviceBinding})?({labelSuffix}{additional})?
     */
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
        if (serviceBindingProtocol != null && serviceBindingProtocol.isEmpty()) {
            return ServiceBinding.fromPortAndProtocol(serviceBindingPort, serviceBindingProtocol);
        }

        return ServiceBinding.fromPort(serviceBindingPort);
    }


}
