package eu.xenit.nuntio.api.platform;

import eu.xenit.nuntio.api.checks.ServiceCheck;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;


@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class PlatformServiceConfiguration {

    @NonNull
    ServiceBinding serviceBinding;

    @Singular
    Set<String> serviceNames;
    @Singular
    Set<String> serviceTags;
    @Singular("serviceMetadata")
    Map<String, String> serviceMetadata;
    @Singular("internalMetadata")
    Map<String, String> internalMetadata;
    @Singular("check")
    Set<ServiceCheck> checks;


    public PlatformServiceConfiguration( ServiceBinding serviceBinding, Set<String> serviceNames,
            Set<String> serviceTags, Map<String, String> serviceMetadata) {
        this(
                serviceBinding,
                Collections.unmodifiableSet(serviceNames),
                Collections.unmodifiableSet(serviceTags),
                Collections.unmodifiableMap(serviceMetadata),
                Collections.emptyMap(),
                Collections.emptySet()
        );
    }


    public PlatformServiceConfiguration withBinding(ServiceBinding newBinding) {
        return toBuilder()
                .serviceBinding(newBinding)
                .build();
    }

    public PlatformServiceConfiguration withInternalMetadata(Map<String, String> additionalMetadata) {
        Map<String, String> internalMetadata = new HashMap<>(this.internalMetadata);
        internalMetadata.putAll(additionalMetadata);
        return toBuilder()
                .internalMetadata(Collections.unmodifiableMap(internalMetadata))
                .build();
    }

    public PlatformServiceConfiguration withInternalMetadata(String key, String value) {
        return withInternalMetadata(Collections.singletonMap(key, value));
    }

}
