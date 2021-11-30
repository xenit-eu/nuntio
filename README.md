# Nuntio (balenae): Docker containers as consul services

Nuntio is a bridge between Docker containers and the Consul service catalog. 
It automatically registers/deregisters labelled Docker containers with Consul as they appear.

## Using Nuntio

Nuntio watches for new Docker containers and inspects them to determine what services they provide. Any services are added to a service registry.

Nuntio is highly extensible and configurable. It is possible to configure different platforms as service sources and to configure different service registries as targets.
The default application is a Docker platform and Consul service registry.

### Configuration options

Nuntio is a Spring Boot application. This means there are many ways to supply configuration parameters:

* Java system properties: use `-D<name>=<value>`
* Environment variables: replace all `.` with `_`
* Command-line arguments: use `--<name>=<value>`

Additional ways to supply configuration options is [documented in the Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

Most specific configuration options are dependent on the selected platform and registry and are documented there.

Only global configuration options are documented here.

<table>
<thead>
<tr>
<th>Option</th>
<th>Default</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>`nuntio.engine.live.enabled`</td><td>`true`</td><td>Enables watching the platform eventstream to immediately react to services changing state.</td>
</tr>
<tr>
<td>`nuntio.engine.live.blocking`</td><td>`true`</td><td>Enables blocking-mode watching of the eventstream. This option can be disabled to fall back to polling-mode operation in case there are problems with waking up blocked threads.</td>
</tr>
<tr>
<td>`nuntio.engine.live.delay`</td><td>`1s`</td><td>Time between checks in polling-mode watching.</td>
</tr>
<tr>
<td>`nuntio.engine.antiEntropy.enabled`</td><td>`true`</td><td>Enables anti-entropy scanning.</td>
</tr>
<tr>
<td>`nuntio.engine.antiEntropy.delay`</td><td>`1m`</td><td>Time between anti-entropy scans.</td>
</tr>
<tr>
<td>`nuntio.engine.checks.heartbeat`</td><td>`true`</td><td>Enable registration of heartbeat check.</td>
</tr>
<tr>
<td>`nuntio.engine.checks.healthcheck`</td><td>`true`</td><td>Enable registration of healthcheck check.</td>
</tr>
</tbody>
</table>


## Docker platform

The Docker platform registers services for containers with certain labels (or environment variables).

### Docker platform configuration

<table>
<thead>
<tr>
<th>Option</th>
<th>Default</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>`nuntio.docker.enabled`</td><td>`true`</td><td>Enable Docker platform</td>
</tr>
<tr>
<td>`nuntio.docker.daemon.host`</td><td>Platform-dependent</td><td>Connection string to the docker daemon (e.g.: `unix:///var/run/docker.sock`, `tcp://some-host:2375`)</td>
</tr>
<tr>
<td>`nuntio.docker.daemon.tlsrVerify`</td><td>`false`</td><td>Enable/disable TLS verification (switch between http & https protocols)</td>
</tr>
<tr>
<td>`nuntio.docker.daemon.certPath`</td><td>`null`</td><td>Path to certificates needed for TLS verification</td>
</tr>
<tr>
<td>`nuntio.docker.bind`</td><td>`PUBLISHED`</td><td>Values: `PUBLISHED`/`INTERNAL`.

Which IP address/port to publish for containers.

* PUBLISHED: Only published ports are used. They are used with the host IP and bound port.
* INTERNAL: All exposed ports are used. They are used with the internal container IP and exposed port. If there are multiple internal container IPs (when the container is attached to multiple networks), no service will be published. To select a specific (e.g. overlay) network, a network filter can be specified with `nuntio.docker.bind.filter`.

</td>
</tr>
<tr>
<td>`nuntio.docker.bind.filter`</td><td>``</td><td>Only applicable for `INTERNAL` binds. Docker filters to determine which network(s) to use IP addresses for. Filters are comma-separated and the same format as in `docker network ls`. Only one network may match per-container.</td>
</tr>

<tr>
<td>`nuntio.docker.nuntioLabel.enabled`</td><td>`true`</td><td>Enables configuring services on Docker containers with labels following the [label specification](#docker-labels-for-nuntio)</td>
</tr>
<tr>
<td>`nuntio.docker.nuntioLabel.prefix`</td><td>`nuntio.vbgn.be`</td><td>Prefix for Nuntio labels.</td>
</tr>
<tr>
<td>`nuntio.docker.registratorCompat.enabled`</td><td>`false`</td><td>Enable compatibility with some registrator labels/environment variables. See [Registrator compatibility](#registrator-compatibility-mode) for details on which parts are implemented.</td>
</tr>
<tr>
<td>`nuntio.docker.registratorCompat.explicit`</td><td>`false`</td><td>Enables `-explicit` mode registrator compatibility, only registering containers that have service configurations present.</td>
</tr>

</tbody>
</table>

### Docker labels for Nuntio

Nuntio needs to know which services to register with which name, tags and metadata. This information is specified with labels on the docker container.

To avoid collisions with other uses for labels, they are namespaced under a configurable prefix (see `nuntio.docker.nuntioLabel.prefix`).

There are two options to specify labels:
 * Global: these labels apply to *all* services (published/exposed ports) on the Docker container.
 * Per-port: these labels apply only to the service with the matching exposed port on the Docker container.
   Port-specific labels are applied by appending the port number to the service prefix: `<prefix>/<port>`.
   If you have an UDP service, you need to specify it on the service prefix: `<prefix>/udp:<port>`

The labels follow these formats:

 * `<prefix>/service`: Comma-separated service names to use for this service. Required to be able to register a service.
 * `<prefix>/tags`: Comma-separated tag names to register with this service.
 * `<prefix>/metadata/<key>`: Metadata value to register with this service.

Label examples

 * `nuntio.vbgn.be/service=my-service,other-service`: All ports of the container are registered under both `my-service` and `other-service`
 * `nuntio.vbgn.be/80/service=lb-front`: Port `80/tcp` of the container is registered under `lb-front`
 * `nuntio.vbgn.be/udp:53/tags=authoritative`: The services for port `53/udp` of the container will have the `authoritative` tag
 * `nuntio.vbgn.be/metadata/my-key=interesting-value`: The services for all ports of the container will have metadata value `my-key=interesting-value`

Some concrete examples:

```yaml
services:
   db1:
      image: postgres
      ports:
        - 1234:5432
      labels:
        nuntio.vbgn.be/service: db
        nuntio.vbgn.be/tags: primary
        nuntio.vbgn.be/metadata/hosted-dbs: metrics,users
   db2:
     image: postgres
     ports:
       - 1235:5432
     labels:
       nuntio.vbgn.be/5432/service: db,db-analytics
       nuntio.vbgn.be/5432/tags: secondary
       nuntio.vbgn.be/5432/metadata/hosted-dbs: metrics,users
   app:
     image: my-app:latest
     ports:
       - 8080
       - 8081
       - 8082
     labels:
       nuntio.vbgn.be/8080/service: app-public
       nuntio.vbgn.be/8080/metadata/published-domain: my-awesome-app.example
       nuntio.vbgn.be/8081/service: app-admin
       nuntio.vbgn.be/metadata/prometheus-scrape: /metrics/prometheus
   custom-dns:
      image: my-dns-server:latest
      ports:
        - 10.5.2.1:53:5300/udp 
      labels:
        nuntio.vbgn.be/udp:5300/service: dns
```

This will register the following services:

 * `db` 
   * instance db1: `ServicePort=1234 Tags=primary Meta[hosted-dbs]=metrics,users`
   * instance db2: `ServicePort=1235 Tags=secondary Meta[hosted-dbs]=metrics,users`
 * `db-analytics`
   * instance db2: `ServicePort=1235 Tags=secondary Meta[hosted-dbs]=metrics,users`
 * `app-public`
   * instance app: `ServicePort=[mapped port for 8080] Meta[published-domain]=my-awesome-app.example Meta[prometheus-scrape]=/metrics/prometheus`
 * `app-admin`
   * instance app: `ServicePort=[mapped port for 8081] Meta[prometheus-scrape]=/metrics/prometheus`
 * `dns`
   * instance custom-dns: `ServiceIp=10.5.2.1 ServicePort=53`  
 

### Registrator compatibility mode

To ease migration from [registrator](https://gliderlabs.github.io/registrator/latest/) to Nuntio, parts of the registrator model are supported.

Compatibility can be enabled with the `nuntio.docker.registratorCompat.enabled=true`.

Following registrator features are supported for both labels and environment variables:

 * `SERVICE_IGNORE`: If this variable is present, no services on this container will be registered.
 * `SERVICE_<port>_IGNORE`: If this variable is present, the service on the port is not registered.
 * `SERVICE_NAME`, `SERVICE_<port>_NAME`: Setting a service name. If `SERVICE_NAME` is used and multiple services are present, the service name will be suffixed with the internal port.
 * `SERVICE_TAGS`, `SERVICE_<port>_TAGS`: Setting service tags.
 * `SERVICE_<meta>`, `SERVICE_<port>_<meta>`: Setting service metadata values.

Following registrator configuration options are supported:
 * `-internal`: Use `nuntio.docker.bind=INTERNAL` to register internal IP and port instead of the host mapped ones.
 * `-explicit`: Use `nuntio.docker.registratorCompat.explicit=true`

## Consul registry

The Consul registry supports registering services to Consul.

## Consul registry configuration

<table>
<thead>
<tr>
<th>Option</th>
<th>Default</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>`nuntio.consul.enabled`</td><td>`true`</td><td>Enable Consul registry</td>
</tr>
<tr>
<td>`nuntio.consul.host`</td><td>`localhost`</td><td>Hostname of the local Consul agent</td>
</tr>
<tr>
<td>`nuntio.consul.port`</td><td>`8500`</td><td>Port of the Consul HTTP API</td>
</tr>
<tr>
<td>`nuntio.consul.token`</td><td>`null`</td><td>ACL token for the Consul HTTP API</td>
</tr>
<tr>
<td>`nuntio.consul.checks.[heartbeat|pause|healthcheck].ttl`</td><td>
* heartbeat: `24h`
* pause: `5m`
* healthcheck: `5m`
</td><td>TTL before a check type expires and is considered critical.</td>
</tr>
<tr>
<td>`nuntio.consul.checks.[heartbeat|pause|healthcheck].deregisterCriticalServiceAfter`</td><td>
* heartbeat: `72h`
* pause: `null`
* healthcheck: `null`
</td><td>Time before a critical check deregisters the service.</td>
</tr>
</tbody>
</table>
