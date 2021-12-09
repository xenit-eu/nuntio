package eu.xenit.nuntio.integtest.jupiter.extensions;

import eu.xenit.nuntio.integtest.containers.ConsulContainer;
import com.ecwid.consul.v1.ConsulClient;

public class ConsulClientParameterResolver extends AbstractClientParameterResolver<ConsulClient, ConsulContainer> {

    public ConsulClientParameterResolver() {
        super(ConsulContainer.class, ConsulClient.class, ConsulContainer::getConsulClient);
    }
}
