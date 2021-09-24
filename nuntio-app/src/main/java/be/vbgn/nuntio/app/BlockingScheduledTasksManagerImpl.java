package be.vbgn.nuntio.app;

import be.vbgn.nuntio.api.management.BlockingScheduledTasksManager;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BlockingScheduledTasksManagerImpl implements BlockingScheduledTasksManager {

    private final Set<Object> blockingTasks = new HashSet<>();

    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    public BlockingScheduledTasksManagerImpl(@Lazy ThreadPoolTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void registerBlockingTask(Object task) {
        if (blockingTasks.add(task)) {
            int currentPoolSize = taskScheduler.getPoolSize();
            int blockingTasksSize = blockingTasks.size();
            int desiredMinPoolSize = blockingTasksSize + 1;
            log.trace(
                    "New blocking task registered. task={}; currentPoolSize={}; blockingTasksSize={}; desiredMinPoolSize={}",
                    task,
                    currentPoolSize, blockingTasksSize, desiredMinPoolSize);
            if (currentPoolSize < desiredMinPoolSize) {
                log.info(
                        "Automatically increasing scheduler pool size from {} to {} because we have {} potentially blocking tasks.",
                        currentPoolSize, desiredMinPoolSize, blockingTasksSize);
                taskScheduler.setPoolSize(desiredMinPoolSize);
            }
        }
    }

}
