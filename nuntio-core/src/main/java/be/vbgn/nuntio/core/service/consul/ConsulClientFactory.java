package be.vbgn.nuntio.core.service.consul;

import com.ecwid.consul.v1.ConsulClient;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ConsulClientFactory implements FactoryBean<ConsulClient> {

    private final ConsulConfig consulConfig;

    @Override
    public ConsulClient getObject() throws Exception {
        return new ConsulClient(consulConfig.getHost(), consulConfig.getPort());
    }

    @Override
    public Class<?> getObjectType() {
        return ConsulClient.class;
    }
}
