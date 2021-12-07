package be.vbgn.nuntio.api.registry.metrics;

public interface RegistryMetricsFactory {
    RegistryMetrics createRegistryMetrics(String registryName);
}
