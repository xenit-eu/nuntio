package be.vbgn.nuntio.platform.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vbgn.nuntio.api.SharedIdentifier;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DockerContainerIdServiceIdentifierTest {

    @Test
    void fromSharedIdentifierRoundtrip() {
        var identifier = new DockerContainerIdServiceIdentifier("aabbcc");
        SharedIdentifier sharedIdentifier = identifier.getSharedIdentifier();

        var optionalIdentifier = DockerContainerIdServiceIdentifier.fromSharedIdentifier(sharedIdentifier);
        assertEquals(Optional.of(identifier), optionalIdentifier);
    }

    @Test
    void fromSharedIdentifierInvalid() {
        SharedIdentifier sharedIdentifier = SharedIdentifier.of("abc");

        var optionalIdentifier = DockerContainerIdServiceIdentifier.fromSharedIdentifier(sharedIdentifier);
        assertEquals(Optional.empty(), optionalIdentifier);
    }

}
