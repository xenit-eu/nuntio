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
        return new SharedIdentifier(Arrays.stream(str.split("-"))
                .map(DECODER::decode)
                .map(String::new)
                .toArray(String[]::new));
    }

    @Override
    public String toString() {
        return Arrays.stream(identifierParts)
                .map(part -> ENCODER.encodeToString(part.getBytes()))
                .collect(Collectors.joining("-"));
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
                .collect(Collectors.joining("-"));
    }
}
