package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.SharedIdentifier;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor
@Value
public class DockerSharedIdentifier {

    private static final String SHARED_IDENTIFIER_ID = DockerSharedIdentifier.class.getName();
    String containerId;
    @Getter(value = AccessLevel.NONE)
    String containerPort;

    public SharedIdentifier toSharedIdentifier() {
        return SharedIdentifier.of(SHARED_IDENTIFIER_ID, containerId, containerPort);
    }

    public static Optional<DockerSharedIdentifier> fromSharedIdentifier(SharedIdentifier sharedIdentifier) {
        if (!sharedIdentifier.getContext().equals(SHARED_IDENTIFIER_ID)) {
            return Optional.empty();
        }
        return Optional.of(new DockerSharedIdentifier(sharedIdentifier.part(0), sharedIdentifier.part(1)));
    }

    public static SharedIdentifier fromContainerIdAndPort(String containerId, String containerPort) {
        return new DockerSharedIdentifier(containerId, containerPort).toSharedIdentifier();
    }

    public static SharedIdentifier fromContainerIdAndPort(String containerId, Optional<String> containerPort) {
        return fromContainerIdAndPort(containerId, containerPort.orElse(""));
    }

    public static SharedIdentifier fromContainerIdAndAddress(String containerId, String containerIp,
            String containerPort) {
        return fromContainerIdAndPort(containerId, containerIp + ":" + containerPort);
    }

}
