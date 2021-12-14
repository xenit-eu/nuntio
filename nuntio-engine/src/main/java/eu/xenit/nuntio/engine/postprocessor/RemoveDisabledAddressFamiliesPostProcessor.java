package eu.xenit.nuntio.engine.postprocessor;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import eu.xenit.nuntio.api.postprocessor.PlatformServicePostProcessor;
import eu.xenit.nuntio.engine.EngineProperties.AddressFamilies;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class RemoveDisabledAddressFamiliesPostProcessor implements PlatformServicePostProcessor {
    private final AddressFamilies familiesToKeep;

    private enum AddressType {
        IPV4,
        IPV6,
    }

    private Optional<AddressType> getAddressType(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if(address instanceof Inet4Address) {
                return Optional.of(AddressType.IPV4);
            } else if(address instanceof Inet6Address) {
                return Optional.of(AddressType.IPV6);
            } else {
                return Optional.empty();
            }
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }

    private boolean shouldBeKept(AddressType addressType) {
        switch (addressType) {
            case IPV4:
                return familiesToKeep.isIpv4();
            case IPV6:
                return familiesToKeep.isIpv6();
        }
        return false;
    }

    @Override
    public Stream<PlatformServiceConfiguration> process(PlatformServiceDescription serviceDescription,
            PlatformServiceConfiguration serviceConfiguration) {

        ServiceBinding serviceBinding = serviceConfiguration.getServiceBinding();

        Optional<AddressType> addressType = serviceBinding.getIp()
                .flatMap(this::getAddressType);

        boolean shouldBeKept = addressType.map(this::shouldBeKept).orElse(false);

        if(shouldBeKept) {
            log.trace("Keeping address {} ({}) because it matches policy {}", serviceBinding.getIp(), addressType, familiesToKeep);
            return Stream.of(serviceConfiguration);
        }

        log.debug("Removing address {} ({}) because it does not match policy {}", serviceBinding.getIp(), addressType, familiesToKeep);

        return Stream.empty();

    }
}
