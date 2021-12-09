package eu.xenit.nuntio.api.identifier;

import eu.xenit.nuntio.api.platform.ServiceBinding;

public final class ServiceIdentifier extends AbstractAnySharedIdentifier<ServiceIdentifier> {
    private ServiceIdentifier(String[] identifierParts) {
        super(ServiceIdentifier::new, identifierParts);
    }

    public static ServiceIdentifier parse(String identifier) {
        return parse(identifier, ServiceIdentifier::new);
    }

    public static ServiceIdentifier of(PlatformIdentifier platformIdentifier, ServiceBinding serviceBinding) {
        return platformIdentifier.transmute(ServiceIdentifier::new)
                .withParts(serviceBinding.getIp().orElse("*"), serviceBinding.getProtocol().orElse("*"), serviceBinding.getPort().orElse("*"));
    }

    public PlatformIdentifier getPlatformIdentifier() {
        return dropParts(3).transmute(PlatformIdentifier::of);
    }

}
