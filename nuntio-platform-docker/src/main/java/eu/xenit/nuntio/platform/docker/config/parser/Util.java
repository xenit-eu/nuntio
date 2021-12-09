package eu.xenit.nuntio.platform.docker.config.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.lang.NonNull;

final class Util {
    private Util() {

    }

    @NonNull
    public static List<String> splitByComma(String input) {
        if (input.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(input.split(","));
    }

}
