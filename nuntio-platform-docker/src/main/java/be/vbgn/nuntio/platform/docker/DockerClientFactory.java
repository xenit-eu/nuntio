package be.vbgn.nuntio.platform.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;

@AllArgsConstructor
@Slf4j
public class DockerClientFactory implements FactoryBean<DockerClient> {

    private DockerProperties config;

    @Override
    public DockerClient getObject() {
        var dockerClientConfigBuilder = new DefaultDockerClientConfig.Builder();
        if (config.getDaemon().getHost() != null && !config.getDaemon().getHost().isEmpty()) {
            dockerClientConfigBuilder.withDockerHost(config.getDaemon().getHost());
        }
        dockerClientConfigBuilder.withDockerTlsVerify(config.getDaemon().isTlsVerify());
        dockerClientConfigBuilder.withDockerCertPath(config.getDaemon().getCertPath());

        DockerClientConfig dockerClientConfig = dockerClientConfigBuilder.build();

        log.info("Creating docker client with config {}", dockerClientConfig);

        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .build();

        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }

    @Override
    public Class<?> getObjectType() {
        return DockerClient.class;
    }

}
