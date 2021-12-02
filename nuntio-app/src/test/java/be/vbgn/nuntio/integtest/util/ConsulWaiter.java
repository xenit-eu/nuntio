package be.vbgn.nuntio.integtest.util;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
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

}
