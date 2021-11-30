package be.vbgn.nuntio.integtest.jupiter.extensions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

public abstract class AbstractClientParameterResolver<T, U> implements ParameterResolver {
    private final Class<U> containerClass;
    private final Class<T> parameterClass;
    private final Function<? super U, ? extends T> clientLocator;

    protected AbstractClientParameterResolver(Class<U> containerClass, Class<T> parameterClass, Function<? super U, ? extends T> clientLocator) {
        this.containerClass = containerClass;
        this.parameterClass = parameterClass;
        this.clientLocator = clientLocator;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterClass.isAssignableFrom(parameterContext.getParameter().getType());
    }

    private Optional<Field> findContainerFields(Class<?> clazz) {
        List<Field> containerFields = ReflectionUtils.findFields(clazz,
                field -> containerClass.isAssignableFrom(field.getType()), HierarchyTraversalMode.BOTTOM_UP);
        switch(containerFields.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(containerFields.get(0));
            default:
                throw new ParameterResolutionException("Multiple "+containerClass+" fields were found in "+clazz);
        }
    }

    @Override
    public T resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        List<Object> testInstances = new ArrayList<>(extensionContext.getRequiredTestInstances().getAllInstances());
        Collections.reverse(testInstances);

        for (Object testInstance : testInstances) {
            var maybeContainerField = findContainerFields(testInstance.getClass());
            if(maybeContainerField.isPresent()) {
                Field containerField = maybeContainerField.get();
                try {
                    containerField.setAccessible(true);
                    return clientLocator.apply((U)containerField.get(testInstance));
                } catch (IllegalAccessException e) {
                    throw new ParameterResolutionException("Failed to resolve "+containerClass+" field", e);
                }
            }
        }

        throw new ParameterResolutionException("No field for "+containerClass+" was found");
    }
}
