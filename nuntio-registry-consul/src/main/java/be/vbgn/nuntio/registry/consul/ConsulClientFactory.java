package be.vbgn.nuntio.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@ConditionalOnProperty("nuntio.consul.enabled")
public class ConsulClientFactory implements FactoryBean<ConsulClient> {

    private final ConsulProperties consulConfig;

    @Override
    public ConsulClient getObject() {
        return new ConsulClient(consulConfig.getHost(), consulConfig.getPort());
    }

    @Override
    public Class<?> getObjectType() {
        return ConsulClient.class;
    }
}
