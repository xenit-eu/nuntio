package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.identifier.PlatformIdentifier;
import be.vbgn.nuntio.api.identifier.ServiceIdentifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
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
