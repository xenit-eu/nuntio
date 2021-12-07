package be.vbgn.nuntio.engine.metrics;

import be.vbgn.nuntio.api.platform.metrics.PlatformMetrics;
import be.vbgn.nuntio.api.platform.metrics.PlatformMetricsFactory;
import be.vbgn.nuntio.api.registry.metrics.RegistryMetrics;
import be.vbgn.nuntio.api.registry.metrics.RegistryMetricsFactory;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MetricsFactory implements PlatformMetricsFactory, RegistryMetricsFactory {

    private final MeterRegistry meterRegistry;

    @Override
    public PlatformMetrics createPlatformMetrics(String platformName) {
        return new PlatformMetricsImpl(meterRegistry, platformName);
    }

    @Override
    public RegistryMetrics createRegistryMetrics(String registryName) {
        return new RegistryMetricsImpl(meterRegistry, registryName);
    }
}
