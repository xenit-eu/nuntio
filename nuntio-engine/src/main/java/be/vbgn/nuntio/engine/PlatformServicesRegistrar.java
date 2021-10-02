package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PlatformServicesRegistrar {

    private final ServicePlatform platform;
    private ServiceMapper serviceMapper;

    public void registerAllServices() {
        for (PlatformServiceDescription platformServiceDescription : platform.findAll()) {
            if (platformServiceDescription.getState() == PlatformServiceState.STOPPED) {
                serviceMapper.unregisterService(platformServiceDescription);
            } else {
                serviceMapper.registerService(platformServiceDescription);
            }
        }
    }

}
