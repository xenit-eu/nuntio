package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.SharedIdentifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class RegistryServiceDescription {

    String name;

    SharedIdentifier sharedIdentifier;

    @Default
    Optional<String> address = Optional.empty();
    String port;

    @Singular
    Set<String> tags;
    @Singular("metadata")
    Map<String, String> metadata;
}
