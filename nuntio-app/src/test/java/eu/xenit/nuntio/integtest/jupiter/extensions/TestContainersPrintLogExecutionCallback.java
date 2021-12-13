package eu.xenit.nuntio.integtest.jupiter.extensions;

import java.lang.reflect.Field;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.testcontainers.junit.jupiter.Container;

@Slf4j
public class TestContainersPrintLogExecutionCallback implements AfterTestExecutionCallback {

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if(context.getExecutionException().isPresent()) {
            List<Field> containerFields = AnnotationUtils.findAnnotatedFields(context.getRequiredTestClass(), Container.class, field -> org.testcontainers.containers.Container.class.isAssignableFrom(field.getType()));
            containerFields.forEach(field -> {
                field.setAccessible(true);
                try {
                    log.info("Logs for {}:\n{}", field.getName(), ((org.testcontainers.containers.Container)field.get(context.getRequiredTestInstance())).getLogs());
                } catch (IllegalAccessException e) {
                    log.error("Failed to get logs for {}", field.getName(), e);
                }
            });
        }

    }
}
