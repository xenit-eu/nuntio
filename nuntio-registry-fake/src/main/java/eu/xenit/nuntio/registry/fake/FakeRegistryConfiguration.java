package eu.xenit.nuntio.registry.fake;

import eu.xenit.nuntio.api.registry.ServiceRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnMissingBean(ServiceRegistry.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class FakeRegistryConfiguration {

    @Bean
    FakeServiceRegistry fakeServiceRegistry() {
        return new FakeServiceRegistry();
    }
}
