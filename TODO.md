# Nuntio to-do list

Rougly ordered in implementation priority

## Custom service checks

Currently, only some fixed TTL checks are supported, and they are registered for all containers in the same way (managed by nuntio).

The docker HEALTHCHECK check could be used for some coarse-grained healthcheck that is propagated to the registry.
It is important to note that a failing HEALTCHECK has other implications for a container that runs on Docker Swarm, it would lead the container to be rescheduled.
(See also: difference between liveness & readiness checks in k8s/spring boot actuators docs).

For our own infrastructure, we would need to support at least TCP & UDP healthchecks.

Proposed label format: `<prefix>/[<port>/]check/<name>/<option>`. This allows *multiple* checks to be configured on a single service and gives the ability to name the checks.

`<option>` allows configuring the options for every check. The option `type` allows to specify the check type: initially `http` or `tcp`.
Other valid options would then be dependent on the check type. All options will be strictly checked, an unrecognized option is an error and will cause the *service* not to be registered.

Not registering a service is the only way that we can give clear feedback, as we don't have any UI to show errors in.
It is more dangerous to not register a check, because then everything would appear to be working until the healthcheck fails and the service is not marked critical in consul.

## Error reports for platform services with errors

When invalid nuntio configuration is set on a platform service, nuntio logs a warning and does not emit any services to register.

That makes it complicated to easily find out the exact problem with your service, especially if you have to crawl through lots logs of other services as well to find the lines applicable to your service.

We can add some engine or integration component that can receive problem reports for platform services.
Problem reports can be handled in multiple ways:

 * Increment a metric (to alert on)
 * Display platform services and errors on a HTML page (for users to look at)
 * Mail service owner about the problem

## Per-container consul ACL token

Consul ACL token is currently configured globally for nuntio. This still allows any created container to register any service it wants without much protection.
To allow ACL tokens to be more narrowly scoped, it can be advantageous to have each container supply their own ACL token.

Proposed label format: `<prefix>/[<port>/]token`. Since different services can be configured on each port, we need to support different tokens for each port as well.
We can inherit the container token if no port-specific token is specified.

An additional chang for nuntio-consul-registry would be to have a default token that is used for services that do not have their own token,
*AND* a management token that is used to unregister services that no longer exist. (No docker container means no labels anymore means no access to the container token anymore)

