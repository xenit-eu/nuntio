package eu.xenit.nuntio.platform.docker;

import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceIdentifier;
import java.util.Optional;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class DockerContainerIdServiceIdentifier implements PlatformServiceIdentifier {

    private static final String SHARED_IDENTIFIER_ID = DockerContainerIdServiceIdentifier.class.getName();

    String containerId;

    @Override
    public PlatformIdentifier getPlatformIdentifier() {
        return PlatformIdentifier.of(SHARED_IDENTIFIER_ID, containerId);
    }

    public static Optional<DockerContainerIdServiceIdentifier> fromPlatformIdentifier(PlatformIdentifier platformIdentifier) {
        if (!platformIdentifier.getContext().equals(SHARED_IDENTIFIER_ID)) {
            return Optional.empty();
        }
        return Optional.of(new DockerContainerIdServiceIdentifier(platformIdentifier.part(0)));
    }
}
