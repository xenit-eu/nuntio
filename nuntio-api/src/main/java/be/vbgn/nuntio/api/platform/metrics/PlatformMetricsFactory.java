package be.vbgn.nuntio.api.platform.metrics;

public interface PlatformMetricsFactory {
    PlatformMetrics createPlatformMetrics(String platformName);
}
