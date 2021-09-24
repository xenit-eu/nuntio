package be.vbgn.nuntio.api;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public final class SharedIdentifier {

    private static final String ENCODED_PART_PREFIX = "b64:";
    private static final String PART_SEPARATOR = "-";
    private final String[] identifierParts;
    private static final Encoder ENCODER = Base64.getEncoder().withoutPadding();
    private static final Decoder DECODER = Base64.getDecoder();

    public static SharedIdentifier of(String... parts) {
        return new SharedIdentifier(parts);
    }

    public String getContext() {
        return part(-1);
    }

    public String part(int i) {
        return identifierParts[i + 1];
    }

    public static SharedIdentifier parse(String str) {
        return new SharedIdentifier(Arrays.stream(str.split(PART_SEPARATOR))
                .map(encodedPart -> {
                    if (!encodedPart.startsWith(ENCODED_PART_PREFIX)) {
                        return encodedPart;
                    }
                    return new String(DECODER.decode(encodedPart.substring(1)));
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

    public SharedIdentifier withParts(String... additionalParts) {
        String[] newArray = new String[identifierParts.length + additionalParts.length];
        for (int i = 0; i < identifierParts.length; i++) {
            newArray[i] = identifierParts[i];
        }
        for (int i = 0; i < additionalParts.length; i++) {
            newArray[identifierParts.length + i] = additionalParts[i];
        }
        return new SharedIdentifier(newArray);
    }

    public String toHumanString() {
        return Arrays.stream(identifierParts)
                .skip(1)
                .collect(Collectors.joining(PART_SEPARATOR));
    }

    @Override
    public String toString() {
        return "SharedIdentifier(" + String.join(" ", identifierParts) + ")";
    }
}
