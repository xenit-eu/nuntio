package be.vbgn.nuntio.platform.fake;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FakePlatformConfiguration {

    @Bean
    FakeServicePlatform fakeServicePlatform() {
        return new FakeServicePlatform();
    }
}
