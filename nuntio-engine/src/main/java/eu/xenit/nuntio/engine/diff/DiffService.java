package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.postprocessor.PlatformServicePostProcessor;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.engine.mapper.PlatformToRegistryMapper;
import eu.xenit.nuntio.engine.platform.DelegatePlatformServiceDescription;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class DiffService {
    private PlatformToRegistryMapper platformToRegistryMapper;
    private List<PlatformServicePostProcessor> postProcessors;

    public Stream<Diff> diff(Map<? extends RegistryServiceIdentifier, RegistryServiceDescription> registryServices, Set<? extends PlatformServiceDescription> platformServiceDescriptions) {
        var processedPlatformServices = postProcessPlatformServices(platformServiceDescriptions);

        Stream.Builder<Diff> diffStream = Stream.builder();

        Set<RegistryServiceIdentifier> stillExistingServices = new HashSet<>();

        for (PlatformServiceDescription platformServiceDescription : processedPlatformServices) {
            for (PlatformServiceConfiguration serviceConfiguration : platformServiceDescription.getServiceConfigurations()) {
                ServiceIdentifier serviceIdentifier = ServiceIdentifier.of(platformServiceDescription.getIdentifier()
                        .getPlatformIdentifier(), serviceConfiguration.getServiceBinding());
                var maybeService = registryServices.keySet()
                        .stream()
                        .filter(registryServiceDescription -> serviceIdentifier.equals(registryServiceDescription.getServiceIdentifier()))
                        .findAny();
                if(maybeService.isEmpty()) {
                    diffStream.add(new AddService(platformServiceDescription, serviceConfiguration));
                } else {
                    var serviceDefinition = registryServices.get(maybeService.get());
                    // Check if the current service definition still matches the one that is derived from the platform
                    var serviceDefinitionFound = platformToRegistryMapper.registryServiceDescriptionsFor(
                                    platformServiceDescription, serviceConfiguration)
                            .anyMatch(Predicate.isEqual(serviceDefinition));
                    stillExistingServices.add(maybeService.get());
                    if (serviceDefinitionFound) {
                        diffStream.add(new EqualService(platformServiceDescription, serviceConfiguration, maybeService.get()));
                    } else {
                        log.warn("Service {} no longer matches definitions for platform {}. Service scheduled for re-registration", maybeService.get(), platformServiceDescription);
                        diffStream.add(new RemoveService(maybeService.get()));
                        diffStream.add(new AddService(platformServiceDescription, serviceConfiguration));
                    }
                }
            }
        }


        Set<RegistryServiceIdentifier> removedServices = new HashSet<>(registryServices.keySet());
        removedServices.removeAll(stillExistingServices);

        removedServices.forEach(removedService -> {
            diffStream.add(new RemoveService(removedService));
        });

        return diffStream.build();
    }

    private Set<? extends PlatformServiceDescription> postProcessPlatformServices(Set<? extends PlatformServiceDescription> platformServiceDescriptions) {
        return platformServiceDescriptions
                .stream()
                .map(this::postProcessPlatformService)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    private Optional<PlatformServiceDescription> postProcessPlatformService(PlatformServiceDescription platformServiceDescription) {
        var serviceConfigurations = platformServiceDescription.getServiceConfigurations().stream();
        for (PlatformServicePostProcessor postProcessor : postProcessors) {
            serviceConfigurations = serviceConfigurations.flatMap(c -> postProcessor.process(platformServiceDescription, c));
        }
        Set<PlatformServiceConfiguration> newServiceConfigurations = serviceConfigurations.collect(Collectors.toSet());
        if(newServiceConfigurations.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new DelegatePlatformServiceDescription(platformServiceDescription, newServiceConfigurations));
        }
    }
}
