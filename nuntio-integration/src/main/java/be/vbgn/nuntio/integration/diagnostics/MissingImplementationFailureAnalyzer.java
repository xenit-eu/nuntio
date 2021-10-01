package be.vbgn.nuntio.integration.diagnostics;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.AbstractInjectionFailureAnalyzer;
import org.springframework.stereotype.Component;

@Component
public class MissingImplementationFailureAnalyzer extends
        AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException> {

    private Set<Class<?>> TYPES_TO_CHECK = Set.of(ServiceRegistry.class, ServicePlatform.class);

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause, String description) {
        if (TYPES_TO_CHECK.contains(cause.getBeanType())) {
            String className = "'" + cause.getBeanType().getName() + "'";
            switch (cause.getNumberOfBeansFound()) {
                case 0:
                    return new FailureAnalysis("No enabled implementation of " + className + " was found.",
                            "* Enable any of the implementations\n* Import the configuration of the implementation\n* Put an implementation on the classpath",
                            cause);
                default:
                    String beanNames = Optional.of(cause)
                            .filter(c -> c instanceof NoUniqueBeanDefinitionException)
                            .map(c -> (NoUniqueBeanDefinitionException) c)
                            .map(c -> String.join(", ", c.getBeanNamesFound()))
                            .map(c -> ": " + c)
                            .orElse(".");
                    return new FailureAnalysis(
                            "Multiple enabled implementations of " + className + " were found" + beanNames,
                            "* Enable only one of the implementations\n* Only import the configuration of the desired implementation\n* Remove the undesired implemenations from the classpath",
                            cause);
            }

        }
        return null;
    }
}
