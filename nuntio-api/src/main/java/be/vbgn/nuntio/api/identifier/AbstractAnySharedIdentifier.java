package be.vbgn.nuntio.api.identifier;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
class AbstractAnySharedIdentifier<T extends AbstractAnySharedIdentifier<T>> implements AnySharedIdentifier<T> {

    @FunctionalInterface
    protected interface SharedIdentifierFactory<T extends AnySharedIdentifier<T>> {
        T fromParts(String[] parts);
    }

    private static final String ENCODED_PART_PREFIX = "b64:";
    private static final String PART_SEPARATOR = "-";
    private final SharedIdentifierFactory<T> factory;
    private final String[] identifierParts;
    private static final Encoder ENCODER = Base64.getEncoder().withoutPadding();
    private static final Decoder DECODER = Base64.getDecoder();

    public String getContext() {
        return part(-1);
    }

    public String part(int i) {
        return identifierParts[i + 1];
    }

    String[] getIdentifierParts() {
        return identifierParts;
    }

    <U extends AnySharedIdentifier<U>> U transmute(SharedIdentifierFactory<U> otherFactory) {
        return otherFactory.fromParts(identifierParts);
    }

    static <R extends AbstractAnySharedIdentifier<R>> R parse(String str, SharedIdentifierFactory<R> factory) {
        return factory.fromParts(Arrays.stream(str.split(PART_SEPARATOR))
                .map(encodedPart -> {
                    if (!encodedPart.startsWith(ENCODED_PART_PREFIX)) {
                        return encodedPart;
                    }
                    return new String(DECODER.decode(encodedPart.substring(ENCODED_PART_PREFIX.length())));
                })
                .toArray(String[]::new));
    }

    public String toMachineString() {
        return Arrays.stream(identifierParts)
                .map(part -> {
                    if (!part.startsWith(ENCODED_PART_PREFIX) && !part.contains(PART_SEPARATOR)) {
                        return part;
                    }
                    return ENCODED_PART_PREFIX + ENCODER.encodeToString(part.getBytes());
                })
                .collect(Collectors.joining(PART_SEPARATOR));
    }

    public T withParts(String... additionalParts) {
        String[] newArray = new String[identifierParts.length + additionalParts.length];
        for (int i = 0; i < identifierParts.length; i++) {
            newArray[i] = identifierParts[i];
        }
        for (int i = 0; i < additionalParts.length; i++) {
            newArray[identifierParts.length + i] = additionalParts[i];
        }
        return factory.fromParts(newArray);
    }

    @Override
    public String[] lastParts(int parts) {
        return Arrays.copyOfRange(identifierParts, identifierParts.length - parts, identifierParts.length);
    }

    public T dropParts(int number) {
        String[] withoutParts = Arrays.copyOfRange(identifierParts, 0, identifierParts.length - number);
        return factory.fromParts(withoutParts);
    }

    public String toHumanString() {
        return Arrays.stream(identifierParts)
                .skip(1)
                .collect(Collectors.joining(PART_SEPARATOR));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"(" + String.join(" ", identifierParts) + ")";
    }
}
