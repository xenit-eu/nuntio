package eu.xenit.nuntio.engine.postprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ForceTagsPostProcessorTest {

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    @Test
    void addTagsWhenNotPresent() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var postProcessor = new ForceTagsPostProcessor(Set.of("my-tag", "my-other-tag"));

        var newConfigurations = postProcessor.process(mock(PlatformServiceDescription.class), configuration).collect(
                Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.toBuilder()
                        .serviceTag("my-tag")
                        .serviceTag("my-other-tag")
                        .build()
        ), newConfigurations);
    }

    @Test
    void addTagsWhenSomePresent() {
        var configuration = PlatformServiceConfiguration.builder()
                .serviceTag("default-tag")
                .serviceTag("my-tag")
                .serviceBinding(ServiceBinding.fromPort(8080))
                .build();

        var postProcessor = new ForceTagsPostProcessor(Set.of("my-tag", "my-other-tag"));

        var newConfigurations = postProcessor.process(mock(PlatformServiceDescription.class), configuration).collect(
                Collectors.toSet());

        assertEquals(Collections.singleton(
                configuration.toBuilder()
                        .serviceTag("default-tag")
                        .serviceTag("my-tag")
                        .serviceTag("my-other-tag")
                        .build()
        ), newConfigurations);
    }

}