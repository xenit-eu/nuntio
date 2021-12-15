package eu.xenit.nuntio.registry.consul.checks;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IpAndPort {
    InetAddress ip;
    int port;

    public static IpAndPort of(String ip, String port) {
        return of(ip, Integer.parseInt(port));
    }

    public static IpAndPort of(String ip, int port) {
        try {
            return new IpAndPort(InetAddress.getByName(ip), port);
        } catch(UnknownHostException e) {
            throw new IllegalArgumentException("Ip is not a valid address", e);
        }
    }

    public String toHost() {
        if(ip instanceof Inet4Address) {
            return ip.getHostAddress()+ ":"+port;
        } else if(ip instanceof Inet6Address) {
            return "["+ip.getHostAddress() + "]:"+port;
        }
        return null;
    }



}
