package be.vbgn.nuntio.api.identifier;

public interface AnySharedIdentifier<T extends AnySharedIdentifier<T>> {

    String getContext();

    String part(int i);

    String[] lastParts(int parts);

    String toMachineString();

    T withParts(String... additionalParts);

    T dropParts(int partsToDrop);

    String toHumanString();

    @Override
    String toString();
}
