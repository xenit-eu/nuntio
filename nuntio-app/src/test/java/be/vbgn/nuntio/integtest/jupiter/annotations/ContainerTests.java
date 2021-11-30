package be.vbgn.nuntio.integtest.jupiter.annotations;

import be.vbgn.nuntio.integtest.jupiter.extensions.ConsulClientParameterResolver;
import be.vbgn.nuntio.integtest.jupiter.extensions.DockerClientParameterResolver;
import be.vbgn.nuntio.integtest.jupiter.extensions.RegistrationCompatInvocationContextProvider;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({  ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({ConsulClientParameterResolver.class, DockerClientParameterResolver.class, RegistrationCompatInvocationContextProvider.class})
public @interface ContainerTests {

}
