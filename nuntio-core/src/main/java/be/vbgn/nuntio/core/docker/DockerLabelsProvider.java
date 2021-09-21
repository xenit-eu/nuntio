package be.vbgn.nuntio.core.docker;

import be.vbgn.nuntio.core.service.ServiceConfiguration.ServiceBinding;
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
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DockerLabelsProvider {

    private final DockerConfig dockerConfig;

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

    @Value
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
        String labelPrefix = dockerConfig.getLabelPrefix() + "/";

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
            serviceBindingProtocol = "tcp";
        }

        return new ServiceBinding(null, Integer.parseInt(serviceBindingPort), serviceBindingProtocol);
    }


}
