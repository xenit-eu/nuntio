package eu.xenit.nuntio.registry.consul.checks;

import com.ecwid.consul.v1.agent.model.NewCheck;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.registry.consul.ConsulServiceIdentifier;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConsulTcpCheck extends ConsulRemoteCheck {

    @Override
    protected void initializeCheck(ConsulServiceIdentifier serviceIdentifier,
            RegistryServiceDescription serviceDescription, IpAndPort host, NewCheck check) {
        check.setTcp(host.toHost());
        check.setNotes(check.getTcp());
    }

    public static class TcpCheckFactory extends RemoteCheckFactory<ConsulTcpCheck> {
        private static final Set<String> CHECK_TYPES = Set.of("consul:tcp", "tcp");

        @Override
        public boolean supportsCheckType(String type) {
            return CHECK_TYPES.contains(type);
        }

        @Override
        protected ConsulTcpCheck createCheck() {
            return new ConsulTcpCheck();
        }
    }
}
