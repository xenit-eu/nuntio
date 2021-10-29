package be.vbgn.nuntio.engine;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.diff.DiffResolver;
import be.vbgn.nuntio.engine.diff.DiffUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PlatformServicesRegistrar {

    private final ServicePlatform platform;
    private final ServiceRegistry registry;
    private final DiffResolver diffResolver;

    public void registerAllServices() {
        DiffUtil.diff(registry.findServices(), platform.findAll())
                .forEach(diffResolver);
    }

}
