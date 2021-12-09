package eu.xenit.nuntio.integtest.jupiter.annotations;

import eu.xenit.nuntio.integtest.containers.RegistrationContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.TestTemplate;

@Target({  ElementType.METHOD, ElementType.ANNOTATION_TYPE  })
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
public @interface CompatTest {
    Class<? extends RegistrationContainer>[] value() default {RegistrationContainer.class};
}
