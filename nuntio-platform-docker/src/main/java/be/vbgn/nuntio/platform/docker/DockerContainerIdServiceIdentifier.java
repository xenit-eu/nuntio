package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceIdentifier;
import java.util.Optional;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class DockerContainerIdServiceIdentifier implements PlatformServiceIdentifier {

    private static final String SHARED_IDENTIFIER_ID = DockerContainerIdServiceIdentifier.class.getName();

    String containerId;

    @Override
    public SharedIdentifier getSharedIdentifier() {
        return SharedIdentifier.of(SHARED_IDENTIFIER_ID, containerId);
    }

    public static Optional<DockerContainerIdServiceIdentifier> fromSharedIdentifier(SharedIdentifier sharedIdentifier) {
        if (!sharedIdentifier.getContext().equals(SHARED_IDENTIFIER_ID)) {
            return Optional.empty();
        }
        return Optional.of(new DockerContainerIdServiceIdentifier(sharedIdentifier.part(0)));
    }
}
