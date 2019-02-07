### S3 Bucket

The Confluence service EC2 instance typically needs access to two buckets: a bucket that hosts the installation-automation scripts and a bucket for backups. The [make_confluence_S3-buckets.tmplt.json](/Templates/make_confluence_S3-buckets.tmplt.json) template _only_ takes care of setting up the bucket for backup-activities. The outputs from this template are used by the IAM Role template to create the requisite S3 bucket access-rules in the resultant IAM policy document.

It is assumed that the toolchain-related software, scripts and other "miscellaneous" data will exist outside of the Confluence deployment-silo. It is further assumed that this content will have the necessary access permissions for use by this project's templates and scripts.
