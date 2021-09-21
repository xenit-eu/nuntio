package be.vbgn.nuntio.core.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

@Component
public class DockerClientFactory implements FactoryBean<DockerClient> {

    @Override
    public DockerClient getObject() {
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .build();

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
