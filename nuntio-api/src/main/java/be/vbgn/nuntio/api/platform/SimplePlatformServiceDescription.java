package be.vbgn.nuntio.api.platform;

import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@Builder(toBuilder = true)
public class SimplePlatformServiceDescription implements PlatformServiceDescription {

    PlatformServiceIdentifier identifier;
    PlatformServiceState state;

    @Builder.Default
    Optional<PlatformServiceHealth> health = Optional.empty();
    @Singular
    Set<PlatformServiceConfiguration> serviceConfigurations;
}
