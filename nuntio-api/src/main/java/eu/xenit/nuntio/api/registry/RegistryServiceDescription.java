package eu.xenit.nuntio.api.registry;

import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class RegistryServiceDescription {
    @NonNull
    ServiceIdentifier serviceIdentifier;
    @NonNull
    PlatformIdentifier platformIdentifier;

    @NonNull
    String name;

    @Default
    Optional<String> address = Optional.empty();
    String port;

    @Singular
    Set<String> tags;
    @Singular("metadata")
    Map<String, String> metadata;
}
