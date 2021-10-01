package be.vbgn.nuntio.registry.fake;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FakeRegistryConfiguration {

    @Bean
    FakeServiceRegistry fakeServiceRegistry() {
        return new FakeServiceRegistry();
    }
}
