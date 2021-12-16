package eu.xenit.nuntio.integtest.util;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.health.HealthChecksForServiceRequest;
import java.util.concurrent.Callable;
import org.awaitility.core.ConditionFactory;
import org.testcontainers.shaded.okhttp3.Call;

public class ConsulWaiter {
    private final ConsulClient consulClient;

    public ConsulWaiter(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    public Callable<Boolean> serviceExists(String service) {
        return () -> consulClient.getCatalogServices(CatalogServicesRequest.newBuilder().build()).getValue().containsKey(service);
    }

    public Callable<Boolean> serviceDoesNotExist(String service) {
        return () -> !serviceExists(service).call();
    }

    public Callable<Boolean> serviceHasChecks(String service) {
        return () -> !consulClient.getHealthChecksForService(service, HealthChecksForServiceRequest.newBuilder().build()).getValue().isEmpty();
    }

    public Callable<Boolean> checkHasOutput(String checkId) {
        return () -> {
            var agentChecks = consulClient.getAgentChecks().getValue();
            if(!agentChecks.containsKey(checkId)) {
                return false;
            }
            return !agentChecks.get(checkId).getOutput().isEmpty();
        };
    }
}
