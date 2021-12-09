package eu.xenit.nuntio.registry.consul.actuators;

import eu.xenit.nuntio.registry.consul.ConsulProperties;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.Self;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@AllArgsConstructor
public class ConsulHealthIndicator implements HealthIndicator {

    private ConsulClient consulClient;
    private ConsulProperties consulProperties;

    @Override
    public Health health() {
        try {
            Self agentSelf = consulClient.getAgentSelf(consulProperties.getToken()).getValue();
            return Health.up()
                    .withDetail("node", agentSelf.getConfig().getNodeName())
                    .withDetail("dc", agentSelf.getConfig().getDatacenter())
                    .build();
        } catch(Exception e) {
            return Health.down(e).build();
        }
    }
}
