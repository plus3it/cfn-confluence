### EC2 Instance

The [make_confluence_EC2-node.tmplt.json](/Templates/make_confluence_EC2-node.tmplt.json) template &mdash; along with deployment-automation helper-scripts &mdash; creates an EC2 Instance.

The created instance:
* Is STIG-hardened by way of [Watchmaker](https://watchmaker.readthedocs.io)
* Has the Confluence binaries installed and onlined
* The onlined Confluence binaries are placed in a "ready for licensing an database-connection configuration state"
