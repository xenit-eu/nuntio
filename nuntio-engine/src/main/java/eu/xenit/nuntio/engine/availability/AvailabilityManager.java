package eu.xenit.nuntio.engine.availability;

public interface AvailabilityManager {
    void registerFailure(Object component);
    void registerSuccess(Object component);
}
