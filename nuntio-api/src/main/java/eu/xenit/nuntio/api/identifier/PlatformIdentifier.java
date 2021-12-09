package eu.xenit.nuntio.api.identifier;

public final class PlatformIdentifier extends AbstractAnySharedIdentifier<PlatformIdentifier> {
    private PlatformIdentifier(String[] identifierParts) {
        super(PlatformIdentifier::new, identifierParts);
    }

    public static PlatformIdentifier parse(String identifier) {
        return parse(identifier, PlatformIdentifier::new);
    }

    public static PlatformIdentifier of(String ...parts) {
        return new PlatformIdentifier(parts);
    }
}
