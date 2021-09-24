package be.vbgn.nuntio.api.platform;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode
public final class ServiceBinding {

    public static final ServiceBinding ANY = new ServiceBinding(null, null, null);

    String ip;
    String port;
    String protocol;

    public static ServiceBinding fromPort(String port) {
        return new ServiceBinding(null, port, "tcp");
    }

    public static ServiceBinding fromPort(int port) {
        return fromPort(Integer.toString(port));
    }

    public static ServiceBinding fromPortAndProtocol(String port, String protocol) {
        return new ServiceBinding(null, port, protocol);
    }

    public static ServiceBinding fromPortAndProtocol(int port, String protocol) {
        return fromPortAndProtocol(Integer.toString(port), protocol);
    }

    public Optional<String> getIp() {
        return Optional.ofNullable(ip);
    }

    public Optional<String> getPort() {
        return Optional.ofNullable(port);
    }

    public Optional<String> getProtocol() {
        return Optional.ofNullable(protocol);
    }

    public ServiceBinding withIp(String ip) {
        return new ServiceBinding(ip, port, protocol);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("ServiceBinding(");
        if (ip != null) {
            sb.append(ip).append(":");
        }
        if (port != null) {
            sb.append(port);
        }
        if (protocol != null) {
            sb.append("/").append(protocol);
        }
        if (ip == null && port == null && protocol == null) {
            sb.append("ANY");
        }

        return sb.append(")").toString();
    }

}
