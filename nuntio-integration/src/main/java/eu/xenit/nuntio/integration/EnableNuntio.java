package eu.xenit.nuntio.integration;

import eu.xenit.nuntio.engine.EngineConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({EngineConfiguration.class, NuntioIntegrationConfiguration.class})
@EnableConfigurationProperties
@EnableScheduling
public @interface EnableNuntio {

}
