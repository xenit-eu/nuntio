package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.SharedIdentifier;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor
@Value
public class DockerServiceSharedIdentifier {

    private static final String SHARED_IDENTIFIER_ID = DockerServiceSharedIdentifier.class.getName();
    String containerId;
    @Getter(value = AccessLevel.NONE)
    String containerPort;

    public SharedIdentifier toSharedIdentifier() {
        return SharedIdentifier.of(SHARED_IDENTIFIER_ID, containerId, containerPort);
    }

    public static Optional<DockerServiceSharedIdentifier> fromSharedIdentifier(SharedIdentifier sharedIdentifier) {
        if (!sharedIdentifier.getContext().equals(SHARED_IDENTIFIER_ID)) {
            return Optional.empty();
        }
        return Optional.of(new DockerServiceSharedIdentifier(sharedIdentifier.part(0), sharedIdentifier.part(1)));
    }

    public static SharedIdentifier fromContainerIdAndPort(String containerId, String containerPort) {
        return new DockerServiceSharedIdentifier(containerId, containerPort).toSharedIdentifier();
    }

    public static SharedIdentifier fromContainerIdAndPort(String containerId, Optional<String> containerPort) {
        return fromContainerIdAndPort(containerId, containerPort.orElse(""));
    }

}
