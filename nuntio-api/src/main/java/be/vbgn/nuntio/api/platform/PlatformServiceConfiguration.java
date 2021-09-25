package be.vbgn.nuntio.api.platform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;


@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlatformServiceConfiguration {

    ServiceBinding serviceBinding;
    Set<String> serviceNames;
    Set<String> serviceTags;
    Map<String, String> serviceMetadata;
    Map<String, String> internalMetadata;

    public PlatformServiceConfiguration(ServiceBinding serviceBinding, Set<String> serviceNames,
            Set<String> serviceTags, Map<String, String> serviceMetadata) {
        this(
                serviceBinding,
                Collections.unmodifiableSet(serviceNames),
                Collections.unmodifiableSet(serviceTags),
                Collections.unmodifiableMap(serviceMetadata),
                Collections.emptyMap()
        );
    }

    public PlatformServiceConfiguration withBinding(ServiceBinding newBinding) {
        return new PlatformServiceConfiguration(newBinding, serviceNames, serviceTags,
                serviceMetadata, internalMetadata);
    }

    public PlatformServiceConfiguration withInternalMetadata(Map<String, String> additionalMetadata) {
        Map<String, String> internalMetadata = new HashMap<>(this.internalMetadata);
        internalMetadata.putAll(additionalMetadata);
        return new PlatformServiceConfiguration(serviceBinding, serviceNames, serviceTags, serviceMetadata,
                Collections.unmodifiableMap(internalMetadata));
    }

    public PlatformServiceConfiguration withInternalMetadata(String key, String value) {
        return withInternalMetadata(Collections.singletonMap(key, value));
    }

}
