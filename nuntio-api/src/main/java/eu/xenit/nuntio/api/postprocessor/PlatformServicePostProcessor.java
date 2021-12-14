package eu.xenit.nuntio.api.postprocessor;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import java.util.stream.Stream;

public interface PlatformServicePostProcessor {
    Stream<PlatformServiceConfiguration> process(PlatformServiceDescription serviceDescription, PlatformServiceConfiguration serviceConfiguration);
}
