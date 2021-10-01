package be.vbgn.nuntio.integration.diagnostics;

import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import java.util.Set;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
            return new FailureAnalysis("No enabled implementation of " + className + " was found.",
                    "* Enable any of the implementations\n* Import the configuration of the implementation\n* Put an implementation on the classpath",
                    cause);

        }
        return null;
    }
}
