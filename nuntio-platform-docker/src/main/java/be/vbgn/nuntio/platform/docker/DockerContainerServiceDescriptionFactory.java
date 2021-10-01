package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.DockerLabelsParser.Label;
import be.vbgn.nuntio.platform.docker.DockerLabelsParser.ParsedLabel;
import be.vbgn.nuntio.platform.docker.config.ServiceConfigurationModifier;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@Component
public class DockerContainerServiceDescriptionFactory {

    private final DockerLabelsParser labelsParser;
    private final List<ServiceConfigurationModifier> configurationModifiers;

    public PlatformServiceDescription createServiceDescription(InspectContainerResponse response) {
        return new DockerContainerServiceDescription(
                response,
                createConfiguration(response)
        );
    }

    private Set<PlatformServiceConfiguration> createConfiguration(InspectContainerResponse response) {
        return createConfiguration(response.getConfig().getLabels())
                .stream()
                .flatMap(configuration -> {
                    var optionalConfig = Stream.of(configuration);
                    for (ServiceConfigurationModifier modifier : configurationModifiers) {
                        optionalConfig = optionalConfig.flatMap(c -> modifier.modifyConfiguration(c, response));
                    }
                    return optionalConfig;
                })
                .collect(Collectors.toSet());
    }

    private Set<PlatformServiceConfiguration> createConfiguration(Map<String, String> labels) {
        var bindingsWithConfiguration = labelsParser.parseLabels(labels)
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getBinding()));

        Set<PlatformServiceConfiguration> serviceConfigurations = new HashSet<>();

        for (Entry<ServiceBinding, List<Entry<ParsedLabel, String>>> bindingListEntry : bindingsWithConfiguration.entrySet()) {
            var services = findLabelOfType(bindingListEntry.getValue(), Label.SERVICE)
                    .map(Entry::getValue)
                    .map(DockerContainerServiceDescriptionFactory::splitByComma);
            var tags = findLabelOfType(bindingListEntry.getValue(), Label.TAGS)
                    .map(Entry::getValue)
                    .map(DockerContainerServiceDescriptionFactory::splitByComma)
                    .orElse(Collections.emptySet());
            var metadata = findLabelsOfType(bindingListEntry.getValue(), Label.METADATA)
                    .collect(Collectors.toMap(entry -> entry.getKey().getAdditional(), Entry::getValue));

            if (services.isEmpty()) {
                log.warn("Binding {} is missing a service but has other configurations.",
                        bindingListEntry.getKey());
            } else if (services.get().isEmpty()) {
                log.warn("Binding {} has no service names.", bindingListEntry.getKey());
            }

            services
                    .filter(service -> !service.isEmpty())
                    .map(service -> new PlatformServiceConfiguration(
                            bindingListEntry.getKey(),
                            service,
                            tags,
                            metadata
                    ))
                    .ifPresent(serviceConfigurations::add);
        }

        return serviceConfigurations;

    }

    private static Stream<Entry<ParsedLabel, String>> findLabelsOfType(List<Entry<ParsedLabel, String>> labels,
            Label type) {
        return labels.stream().filter(e -> e.getKey().getLabelKind() == type);
    }

    private static Optional<Entry<ParsedLabel, String>> findLabelOfType(List<Entry<ParsedLabel, String>> labels,
            Label type) {
        return findLabelsOfType(labels, type).findFirst();
    }

    @NonNull
    private static Set<String> splitByComma(String input) {
        if (input.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(input.split(",")));
    }

}
