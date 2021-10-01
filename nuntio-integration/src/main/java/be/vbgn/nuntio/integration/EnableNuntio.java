package be.vbgn.nuntio.integration;

import be.vbgn.nuntio.engine.EngineConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({NuntioIntegrationConfiguration.class, EngineConfiguration.class})
@EnableConfigurationProperties
@EnableScheduling
public @interface EnableNuntio {

}
