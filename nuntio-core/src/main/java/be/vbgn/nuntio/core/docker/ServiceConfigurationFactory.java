package be.vbgn.nuntio.core.docker;

import be.vbgn.nuntio.core.docker.DockerLabelsProvider.Label;
import be.vbgn.nuntio.core.docker.DockerLabelsProvider.ParsedLabel;
import be.vbgn.nuntio.core.service.Service;
import be.vbgn.nuntio.core.service.Service.Identifier;
import be.vbgn.nuntio.core.service.ServiceConfiguration;
import be.vbgn.nuntio.core.service.ServiceConfiguration.ServiceBinding;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class ServiceConfigurationFactory {

    private final DockerLabelsProvider labelsProvider;

    public Optional<Service.Identifier> createIdentifierFromEvent(Event event) {
        return Optional.ofNullable(event)
                .map(Event::getActor)
                .flatMap(actor -> {
                    var nameOpt = Optional.ofNullable(actor.getAttributes())
                            .map(attributes -> attributes.get("name"));
                    var idOpt = Optional.ofNullable(actor.getId());
                    return nameOpt.flatMap(name -> idOpt.map(id -> new Identifier(name, id)));
                });
    }

    public Optional<Service.Identifier> createIdentifierFromContainer(Container container) {
        return Optional.of(new Identifier(container.getNames()[0].substring(1), container.getId()));
    }

    public Set<ServiceConfiguration> createConfigurationFromEvent(Event event) {
        return Optional.ofNullable(event)
                .map(Event::getActor)
                .map(EventActor::getAttributes)
                .map(this::createConfigurationFromLabels)
                .orElse(Collections.emptySet());
    }

    public Set<ServiceConfiguration> createConfigurationFromContainer(Container container) {
        return createConfigurationFromLabels(container.getLabels());
    }

    private Set<ServiceConfiguration> createConfigurationFromLabels(Map<String, String> labels) {
        var bindingsWithConfiguration = labelsProvider.parseLabels(labels)
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getBinding()));

        Set<ServiceConfiguration> serviceConfigurations = new HashSet<>();

        for (Entry<ServiceBinding, List<Entry<ParsedLabel, String>>> bindingListEntry : bindingsWithConfiguration.entrySet()) {
            var services = findLabelOfType(bindingListEntry.getValue(), Label.SERVICE)
                    .map(Entry::getValue)
                    .map(ServiceConfigurationFactory::splitByComma);
            var tags = findLabelOfType(bindingListEntry.getValue(), Label.TAGS)
                    .map(Entry::getValue)
                    .map(ServiceConfigurationFactory::splitByComma)
                    .orElse(Collections.emptySet());
            var metadata = findLabelsOfType(bindingListEntry.getValue(), Label.METADATA)
                    .collect(Collectors.toMap(entry -> entry.getKey().getAdditional(), Entry::getValue));

            services.map(service -> new ServiceConfiguration(bindingListEntry.getKey(), service, tags, metadata))
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
    private static Set<String> splitByComma(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(input.split(",")));
    }

}
