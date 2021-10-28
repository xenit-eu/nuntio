package be.vbgn.nuntio.api.identifier;

import static org.junit.jupiter.api.Assertions.*;

import be.vbgn.nuntio.api.platform.ServiceBinding;
import org.junit.jupiter.api.Test;

class ServiceIdentifierTest {
    @Test
    void toPlatform() {
        var platformIdentifier = PlatformIdentifier.of("some", "platform", "name");
        var serviceBinding = ServiceBinding.fromPortAndProtocol(8888, "udp");
        ServiceIdentifier serviceIdentifier = ServiceIdentifier.of(platformIdentifier, serviceBinding);

        assertEquals(platformIdentifier, serviceIdentifier.getPlatformIdentifier());
    }

}