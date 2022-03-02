package eu.xenit.nuntio.platform.fake;

import eu.xenit.nuntio.api.platform.ServicePlatform;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnMissingBean(ServicePlatform.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class FakePlatformConfiguration {

    @Bean
    FakeServicePlatform fakeServicePlatform() {
        return new FakeServicePlatform();
    }
}
