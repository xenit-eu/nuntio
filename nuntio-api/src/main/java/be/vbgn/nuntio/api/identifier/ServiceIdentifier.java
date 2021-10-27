package be.vbgn.nuntio.api.identifier;

import be.vbgn.nuntio.api.platform.ServiceBinding;

public final class ServiceIdentifier extends AbstractAnySharedIdentifier<ServiceIdentifier> {
    private ServiceIdentifier(String[] identifierParts) {
        super(ServiceIdentifier::new, identifierParts);
    }

    public static ServiceIdentifier parse(String identifier) {
        return parse(identifier, ServiceIdentifier::new);
    }

    public static ServiceIdentifier of(PlatformIdentifier platformIdentifier, ServiceBinding serviceBinding) {
        return new ServiceIdentifier(platformIdentifier.getIdentifierParts()).withParts(serviceBinding.getIp().orElse("*"), serviceBinding.getProtocol().orElse("*"), serviceBinding.getPort().orElse("*"));
    }
}
