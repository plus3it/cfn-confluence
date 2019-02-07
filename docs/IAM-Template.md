### IAM Role

The [make_confluence_IAM-instance.tmplt.json](/Templates/make_confluence_IAM-instance.tmplt.json) file sets up an IAM role. This role is attached to the Confluence-hosting EC2 instance. The primary purpose of the IAM role is to grant access from the EC2 instance to an associated S3 bucket.

An example of the resultant IAM policy can be viewed [here](/docs/IAMpolicyExample.md)
