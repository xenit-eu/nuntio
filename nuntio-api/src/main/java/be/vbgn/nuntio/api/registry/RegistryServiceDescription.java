package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.SharedIdentifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Value;

@Value
public class RegistryServiceDescription {

    String name;

    SharedIdentifier sharedIdentifier;

    Optional<String> address;
    String port;

    Set<String> tags;
    Map<String, String> metadata;
}
