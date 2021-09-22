package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlatformToRegistryMapper {

    private ServicePlatform platform;
    private ServiceRegistry registry;

    public Set<RegistryServiceDescription> createServices(PlatformServiceDescription serviceDescription) {
        return serviceDescription.getServiceConfigurations()
                .stream()
                .flatMap(configuration -> configuration.getServiceNames()
                        .stream()
                        .map(serviceName -> new RegistryServiceDescription(
                                serviceName,
                                configuration.getSharedIdentifier(),
                                configuration.getServiceBinding().getIp(),
                                configuration.getServiceBinding().getPort().orElseThrow(),
                                configuration.getServiceTags(),
                                configuration.getServiceMetadata()
                        ))
                )
                .collect(Collectors.toSet());
    }

}
