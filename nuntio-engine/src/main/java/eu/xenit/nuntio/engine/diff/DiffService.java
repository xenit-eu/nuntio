package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.PlatformServiceState;
import eu.xenit.nuntio.api.postprocessor.PlatformServicePostProcessor;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.engine.platform.DelegatePlatformServiceDescription;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DiffService {
    private List<PlatformServicePostProcessor> postProcessors;

    public Stream<Diff> diff(Set<? extends RegistryServiceIdentifier> registryServices, Set<? extends PlatformServiceDescription> platformServiceDescriptions) {
        var processedPlatformServices = postProcessPlatformServices(platformServiceDescriptions);

        Stream.Builder<Diff> diffStream = Stream.builder();

        Set<RegistryServiceIdentifier> stillExistingServices = new HashSet<>();

        for (PlatformServiceDescription platformServiceDescription : processedPlatformServices) {
            for (PlatformServiceConfiguration serviceConfiguration : platformServiceDescription.getServiceConfigurations()) {
                ServiceIdentifier serviceIdentifier = ServiceIdentifier.of(platformServiceDescription.getIdentifier()
                        .getPlatformIdentifier(), serviceConfiguration.getServiceBinding());
                var maybeService = registryServices.stream().filter(registryServiceIdentifier -> serviceIdentifier.equals(registryServiceIdentifier.getServiceIdentifier())).findAny();
                if(maybeService.isEmpty()) {
                    diffStream.add(new AddService(platformServiceDescription, serviceConfiguration));
                } else {
                    stillExistingServices.add(maybeService.get());
                    diffStream.add(new EqualService(platformServiceDescription, serviceConfiguration, maybeService.get()));
                }
            }
        }


        Set<RegistryServiceIdentifier> removedServices = new HashSet<>(registryServices);
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
