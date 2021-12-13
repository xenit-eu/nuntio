package eu.xenit.nuntio.integtest.jupiter.annotations;

import eu.xenit.nuntio.integtest.jupiter.extensions.ConsulClientParameterResolver;
import eu.xenit.nuntio.integtest.jupiter.extensions.DockerClientParameterResolver;
import eu.xenit.nuntio.integtest.jupiter.extensions.RegistrationCompatInvocationContextProvider;
import eu.xenit.nuntio.integtest.jupiter.extensions.TestContainersPrintLogExecutionCallback;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({  ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({ConsulClientParameterResolver.class, DockerClientParameterResolver.class, RegistrationCompatInvocationContextProvider.class,
        TestContainersPrintLogExecutionCallback.class})
public @interface ContainerTests {

}
