package be.vbgn.nuntio.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SharedIdentifierTest {

    static Stream<Arguments> factory() {
        return Stream.of(
                Arguments.of(Arrays.asList("abc", "def", "ghi"), "abc-def-ghi"),
                Arguments.of(Arrays.asList("aaa=+:", "b-s"), "aaa=+:-b64:Yi1z"),
                Arguments.of(Arrays.asList("aaa=+:", "b64:YQo", "zz"), "aaa=+:-b64:YjY0OllRbw-zz")
        );
    }

    @ParameterizedTest
    @MethodSource("factory")
    void toMachineString(List<String> parts, String encoded) {
        SharedIdentifier sharedIdentifier = SharedIdentifier.of(parts.toArray(new String[0]));
        assertEquals(encoded, sharedIdentifier.toMachineString());
    }

    @ParameterizedTest
    @MethodSource("factory")
    void parse(List<String> parts, String encoded) {
        SharedIdentifier sharedIdentifier = SharedIdentifier.parse(encoded);
        assertEquals(SharedIdentifier.of(parts.toArray(new String[0])), sharedIdentifier);
    }

    @ParameterizedTest
    @MethodSource("factory")
    void toHumanString(List<String> parts) {
        SharedIdentifier sharedIdentifier = SharedIdentifier.of(parts.toArray(new String[0]));
        assertEquals(String.join("-", parts.subList(1, parts.size())), sharedIdentifier.toHumanString());
    }

    @Test
    void part() {
        SharedIdentifier sharedIdentifier = SharedIdentifier.of("abc", "def", "ghi");

        assertEquals("abc", sharedIdentifier.getContext());
        assertEquals("def", sharedIdentifier.part(0));
        assertEquals("ghi", sharedIdentifier.part(1));
    }

    @Test
    void withParts() {
        SharedIdentifier sharedIdentifier = SharedIdentifier.of("abc", "def", "ghi").withParts("mno");

        assertEquals("abc", sharedIdentifier.getContext());
        assertEquals("def", sharedIdentifier.part(0));
        assertEquals("ghi", sharedIdentifier.part(1));
        assertEquals("mno", sharedIdentifier.part(2));
    }

}
