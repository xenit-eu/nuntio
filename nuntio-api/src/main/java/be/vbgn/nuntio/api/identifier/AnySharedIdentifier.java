package be.vbgn.nuntio.api.identifier;

public interface AnySharedIdentifier<T extends AnySharedIdentifier<T>> {

    String getContext();

    String part(int i);

    String toMachineString();

    T withParts(String... additionalParts);

    String toHumanString();

    @Override
    String toString();
}
