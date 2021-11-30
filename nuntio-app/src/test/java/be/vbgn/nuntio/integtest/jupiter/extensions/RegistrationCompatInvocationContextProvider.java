package be.vbgn.nuntio.integtest.jupiter.extensions;

import be.vbgn.nuntio.integtest.jupiter.annotations.CompatTest;
import be.vbgn.nuntio.integtest.containers.RegistrationContainer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

public class RegistrationCompatInvocationContextProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        Method templateMethod =  context.getRequiredTestMethod();
        return AnnotationUtils.findAnnotation(templateMethod, CompatTest.class).isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Method templateMethod = context.getRequiredTestMethod();
        CompatTest compatTest = AnnotationUtils.findAnnotation(templateMethod, CompatTest.class).get();
        List<Field> fieldsWithRegistrationContainers = ReflectionUtils.findFields(context.getRequiredTestClass(), field -> Arrays.stream(compatTest.value()).anyMatch(type -> type.isAssignableFrom(field.getType())), HierarchyTraversalMode.TOP_DOWN);
        return fieldsWithRegistrationContainers.stream().map(field -> {
            field.setAccessible(true);
            return invocationContextForContainer(field);
        });

    }

    private TestTemplateInvocationContext invocationContextForContainer(Field field) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return field.getType().getSimpleName() + "["+field.getName()+"]";
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return Arrays.asList(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext,
                            ExtensionContext extensionContext) throws ParameterResolutionException {
                        return RegistrationContainer.class.isAssignableFrom(parameterContext.getParameter().getType());
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                            throws ParameterResolutionException {
                        try {
                            return (RegistrationContainer) field.get(extensionContext.getRequiredTestInstance());
                        } catch (IllegalAccessException e) {
                            throw new ParameterResolutionException("Cant resolve container", e);
                        }
                    }
                }, new BeforeTestExecutionCallback() {
                    @Override
                    public void beforeTestExecution(ExtensionContext context) throws Exception {
                        var registrationContainer = (RegistrationContainer) field.get(
                                context.getRequiredTestInstance());
                        registrationContainer.start();
                    }
                }, new AfterTestExecutionCallback() {
                    @Override
                    public void afterTestExecution(ExtensionContext context) throws Exception {
                        var registrationContainer = (RegistrationContainer) field.get(context.getRequiredTestInstance());
                        registrationContainer.stop();
                    }
                });
            }
        };
    }

}
