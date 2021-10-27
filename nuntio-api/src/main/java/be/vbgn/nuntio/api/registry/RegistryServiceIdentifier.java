package be.vbgn.nuntio.api.registry;

import be.vbgn.nuntio.api.identifier.PlatformIdentifier;
import be.vbgn.nuntio.api.identifier.ServiceIdentifier;

public interface RegistryServiceIdentifier {

    PlatformIdentifier getPlatformIdentifier();
    ServiceIdentifier getServiceIdentifier();
}
