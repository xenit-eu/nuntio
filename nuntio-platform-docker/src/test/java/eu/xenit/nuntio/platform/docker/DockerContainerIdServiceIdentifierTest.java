package eu.xenit.nuntio.platform.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DockerContainerIdServiceIdentifierTest {

    @Test
    void fromPlatformIdentifierRoundtrip() {
        var identifier = new DockerContainerIdServiceIdentifier("aabbcc");
        PlatformIdentifier platformIdentifier = identifier.getPlatformIdentifier();

        var optionalIdentifier = DockerContainerIdServiceIdentifier.fromPlatformIdentifier(platformIdentifier);
        assertEquals(Optional.of(identifier), optionalIdentifier);
    }

    @Test
    void fromPlatformIdentifierInvalid() {
        PlatformIdentifier platformIdentifier = PlatformIdentifier.of("abc");

        var optionalIdentifier = DockerContainerIdServiceIdentifier.fromPlatformIdentifier(platformIdentifier);
        assertEquals(Optional.empty(), optionalIdentifier);
    }

}
