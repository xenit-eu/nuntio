package be.vbgn.nuntio.api.registry;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CheckType {
    HEARTBEAT("Nuntio heartbeat", "To automatically unregister service when nuntio does not know it"),
    HEALTHCHECK("Container healthcheck", "Tracks container healthcheck status (starting=warning)"),
    PAUSE("Container pause", "Fails when container is paused");

    final String title;
    final String description;


    public String getCheckId() {
        return name().toLowerCase();
    }
}
