package be.vbgn.nuntio.api.platform;

import java.util.Optional;
import java.util.Set;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class SimplePlatformServiceDescription implements PlatformServiceDescription {

    PlatformServiceIdentifier identifier;
    PlatformServiceState state;
    Optional<PlatformServiceHealth> health;
    Set<PlatformServiceConfiguration> serviceConfigurations;
}
