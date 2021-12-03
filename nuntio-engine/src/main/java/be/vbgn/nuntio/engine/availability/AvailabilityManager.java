package be.vbgn.nuntio.engine.availability;

public interface AvailabilityManager {
    void registerFailure(Object component);
    void registerSuccess(Object component);
}
