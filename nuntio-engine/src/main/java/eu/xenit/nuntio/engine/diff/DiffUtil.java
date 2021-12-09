package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.PlatformServiceState;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiffUtil {
    public static Stream<Diff> diff(Set<? extends RegistryServiceIdentifier> registryServices, Set<? extends PlatformServiceDescription> platformServiceDescriptions) {
        var withoutStopped = platformServiceDescriptions.stream()
                .filter(platformServiceDescription -> platformServiceDescription.getState() != PlatformServiceState.STOPPED)
                .collect(Collectors.toSet());

        Stream.Builder<Diff> diffStream = Stream.builder();

        Set<RegistryServiceIdentifier> stillExistingServices = new HashSet<>();

        for (PlatformServiceDescription platformServiceDescription : withoutStopped) {
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


}
