package eu.xenit.nuntio.engine.postprocessor;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.postprocessor.PlatformServicePostProcessor;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ForceTagsPostProcessor implements PlatformServicePostProcessor {
    private final Set<String> tags;

    @Override
    public Stream<PlatformServiceConfiguration> process(PlatformServiceDescription serviceDescription,
            PlatformServiceConfiguration serviceConfiguration) {
        Set<String> newTags = new HashSet<>(serviceConfiguration.getServiceTags());
        newTags.addAll(tags);
        return Stream.of(serviceConfiguration.toBuilder().serviceTags(newTags).build());
    }
}
