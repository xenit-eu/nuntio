package be.vbgn.nuntio.integtest.jupiter.annotations;

import be.vbgn.nuntio.integtest.containers.NuntioContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({  ElementType.METHOD, ElementType.ANNOTATION_TYPE  })
@Retention(RetentionPolicy.RUNTIME)
@CompatTest(NuntioContainer.class)
public @interface NuntioTest {

}
