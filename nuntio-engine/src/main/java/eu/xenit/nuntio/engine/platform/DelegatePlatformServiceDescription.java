package eu.xenit.nuntio.engine.platform;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Delegate;

@Value
public class DelegatePlatformServiceDescription implements PlatformServiceDescription {
    @Getter(AccessLevel.NONE)
    @Delegate PlatformServiceDescription platformServiceDescription;

    Set<PlatformServiceConfiguration> serviceConfigurations;
}
