package be.vbgn.nuntio.integtest.jupiter.annotations;

import be.vbgn.nuntio.integtest.containers.RegistrationContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;
import org.junit.jupiter.api.TestTemplate;

@Target({  ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
public @interface CompatTest {
    Class<? extends RegistrationContainer>[] value() default {RegistrationContainer.class};
}
