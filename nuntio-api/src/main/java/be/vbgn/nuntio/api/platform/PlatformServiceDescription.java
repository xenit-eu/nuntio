package be.vbgn.nuntio.api.platform;

import java.util.Optional;
import java.util.Set;

public interface PlatformServiceDescription {

    PlatformServiceIdentifier getIdentifier();

    PlatformServiceState getState();

    Optional<PlatformServiceHealth> getHealth();

    Set<PlatformServiceConfiguration> getServiceConfigurations();
}
