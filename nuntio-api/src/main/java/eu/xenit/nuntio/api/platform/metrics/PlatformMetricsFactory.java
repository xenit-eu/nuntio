package eu.xenit.nuntio.api.platform.metrics;

public interface PlatformMetricsFactory {
    PlatformMetrics createPlatformMetrics(String platformName);
}
