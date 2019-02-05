### "Classic" Elastic LoadBalancer

The Confluence EC2 instance launched by this project should be deployed into a VPC's private subnet. The Elastic LoadBalancer &mdash; created by the [make_confluence_ELBv2-pub.tmplt.json](/Templates/make_confluence_ELBv2-pub.tmplt.json) template &mdash; provides the public-facing ingress-/egress-point to the Confluence service-deployment. This ELB provides the bare-minimum transit services required for the Confluence web service to be usable from client requests arriving via the public Internet.
