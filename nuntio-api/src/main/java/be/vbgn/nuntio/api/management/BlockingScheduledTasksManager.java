package be.vbgn.nuntio.api.management;

public interface BlockingScheduledTasksManager {

    void registerBlockingTask(Object task);
}
