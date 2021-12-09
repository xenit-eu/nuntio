package eu.xenit.nuntio.api.registry;

import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import eu.xenit.nuntio.api.identifier.ServiceIdentifier;

public interface RegistryServiceIdentifier {

    default PlatformIdentifier getPlatformIdentifier() {
        return getServiceIdentifier().getPlatformIdentifier();
    }

    ServiceIdentifier getServiceIdentifier();
}
