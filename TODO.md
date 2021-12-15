# Nuntio to-do list

Rougly ordered in implementation priority

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

