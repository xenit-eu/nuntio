package eu.xenit.nuntio.platform.docker.config.modifier;

import com.github.dockerjava.api.command.InspectContainerResponse;
import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveAnyLocalAddressConfigurationModifier implements ServiceConfigurationModifier {

    private static boolean isAnyLocalIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    @Override
    public Stream<PlatformServiceConfiguration> modifyConfiguration(PlatformServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse) {
        ServiceBinding serviceBinding = configuration.getServiceBinding();
        if(serviceBinding.getIp()
                .map(RemoveAnyLocalAddressConfigurationModifier::isAnyLocalIp)
                .orElse(false)
        ) {
            log.debug("Removing ip {} because it is an any bind address", serviceBinding.getIp());
            return Stream.of(configuration.withBinding(serviceBinding.withIp(null)));
        }
        log.trace("Keeping ip {} because it is not an any bind address", serviceBinding.getIp());
        return Stream.of(configuration);
    }
}
