### Security Groups

The [make_confluence_SGs.tmplt.jso](/Templates/make_confluence_SGs.tmplt.jso) file sets up the security group used to gate network-access to the Confluence elements. The Confluence design assumes that the entirety of the Confluence-deployment exists within a security-silo. This silo contains only the Confluence-service elements. The security-group created by this template is designed to foster communication between service-elements while allowing network-ingress and -egress to the silo _only_ through the Internet-facing load-balancer.
