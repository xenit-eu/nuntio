package be.vbgn.nuntio.api.platform;

import be.vbgn.nuntio.api.SharedIdentifier;
import java.util.Map;
import java.util.Set;
import lombok.Value;


@Value
public class PlatformServiceConfiguration {

    SharedIdentifier sharedIdentifier;

    ServiceBinding serviceBinding;
    Set<String> serviceNames;
    Set<String> serviceTags;
    Map<String, String> serviceMetadata;


    public PlatformServiceConfiguration withBinding(ServiceBinding newBinding) {
        return new PlatformServiceConfiguration(sharedIdentifier, newBinding, serviceNames, serviceTags,
                serviceMetadata);
    }

    public PlatformServiceConfiguration withIdentifier(SharedIdentifier sharedIdentifier) {
        return new PlatformServiceConfiguration(sharedIdentifier, serviceBinding, serviceNames, serviceTags,
                serviceMetadata);
    }
}
