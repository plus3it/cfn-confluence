The following policy document will be annotated at a later date:

~~~
{
          "Version": "2012-10-17",
          "Statement": [
            {
              "Action": [
                "s3:*"
              ],
              "Effect": "Allow",
              "Resource": [
                {
                  "Fn::Join": [
                    "",
                    [
                      { "Ref": "BackupBucketArn" },
                      ""
                    ]
                  ]
                },
                {
                  "Fn::Join": [
                    "",
                    [
                      { "Ref": "BackupBucketArn" },
                      "/*"
                    ]
                  ]
                }
              ],
              "Sid": "BackupsAccess"
            },
            {
              "Action": [
                "cloudformation:DescribeStackResource",
                "cloudformation:SignalResource"
              ],
              "Effect": "Allow",
              "Resource": [
                "*"
              ],
              "Sid": "ASGsupport"
            },
            {
              "Action": [
                "cloudwatch:PutMetricData",
                "ds:CreateComputer",
                "ds:DescribeDirectories",
                "ec2:DescribeInstanceStatus",
                "ec2messages:AcknowledgeMessage",
                "ec2messages:DeleteMessage",
                "ec2messages:FailMessage",
                "ec2messages:GetEndpoint",
                "ec2messages:GetMessages",
                "ec2messages:SendReply",
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:DescribeLogGroups",
                "logs:DescribeLogStreams",
                "logs:PutLogEvents",
                "ssm:DescribeAssociation",
                "ssm:GetDeployablePatchSnapshotForInstance",
                "ssm:GetDocument",
                "ssm:GetParameters",
                "ssm:ListInstanceAssociations",
                "ssm:ListAssociations",
                "ssm:PutInventory",
                "ssm:UpdateAssociationStatus",
                "ssm:UpdateInstanceAssociationStatus",
                "ssm:UpdateInstanceInformation"
              ],
              "Effect": "Allow",
              "Resource": "*",
              "Sid": "MiscEnablement"
            },
            {
              "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:ListBucketMultipartUploads",
                "s3:AbortMultipartUpload",
                "s3:ListMultipartUploadParts"
              ],
              "Effect": "Allow",
              "Resource": {
                "Fn::Join": [
                  "",
                  [
                    "arn:",
                    { "Ref": "AWS::Partition" },
                    ":s3:::ssm-",
                    { "Ref": "AWS::AccountId" },
                    "/*"
                  ]
                ]
              },
              "Sid": "AcctSsmBucket"
            },
            {
              "Action": "s3:ListBucket",
              "Effect": "Allow",
              "Resource": {
                "Fn::Join": [
                  "",
                  [
                    "arn:",
                    { "Ref": "AWS::Partition" },
                    ":s3:::amazon-ssm-packages-*"
                  ]
                ]
              },
              "Sid": "GetSsmPkgs"
            }
          ]
}
~~~
