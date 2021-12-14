package eu.xenit.nuntio.engine.postprocessor;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.PlatformServiceState;
import eu.xenit.nuntio.api.postprocessor.PlatformServicePostProcessor;
import java.util.stream.Stream;

public class RemoveStoppedPlatformsPostProcessor implements PlatformServicePostProcessor {

    @Override
    public Stream<PlatformServiceConfiguration> process(PlatformServiceDescription serviceDescription,
            PlatformServiceConfiguration serviceConfiguration) {
        if(serviceDescription.getState() == PlatformServiceState.STOPPED) {
            return Stream.empty();
        }
        return Stream.of(serviceConfiguration);
    }
}
