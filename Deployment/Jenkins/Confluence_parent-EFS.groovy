pipeline {

    agent any

    options {
        buildDiscarder(
            logRotator(
                numToKeepStr: '10',
                daysToKeepStr: '30',
                artifactDaysToKeepStr: '30',
                artifactNumToKeepStr: '10'
            )
        )
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        AWS_DEFAULT_REGION = "${AwsRegion}"
        AWS_CA_BUNDLE = '/etc/pki/tls/certs/ca-bundle.crt'
        REQUESTS_CA_BUNDLE = '/etc/pki/tls/certs/ca-bundle.crt'
    }

    parameters {
        string(name: 'AwsRegion', defaultValue: 'us-east-1', description: 'Amazon region to deploy resources into')
        string(name: 'AwsCred', description: 'Jenkins-stored AWS credential with which to execute cloud-layer commands')
        string(name: 'GitCred', description: 'Jenkins-stored Git credential with which to execute git commands')
        string(name: 'GitProjUrl', description: 'SSH URL from which to download the Confluence git project')
        string(name: 'GitProjBranch', description: 'Project-branch to use from the Confluence git project')
        string(name: 'CfnStackRoot', description: 'Unique token to prepend to all stack-element names')
        string(name: 'AdminPubkeyURL', description: 'URL to the administrator pub keys')
        string(name: 'AmiId', description: 'ID of the AMI to launch')
        string(name: 'AppVolumeDevice', defaultValue: 'false', description: 'Whether to mount an extra EBS volume. Leave as default (\"false\") to launch without an extra application volume')
        string(name: 'AppVolumeMountPath', defaultValue: '/var/jenkins', description: 'Filesystem path to mount the extra app volume. Ignored if \"AppVolumeDevice\" is blank')
        string(name: 'AppVolumeSize', defaultValue: '20', description: 'Size in GB of the EBS volume to create. Ignored if \"AppVolumeDevice\" is blank')
        string(name: 'AppVolumeType', defaultValue: 'gp2', description: 'Type of EBS volume to create. Ignored if \"AppVolumeDevice\" is blank')
        string(name: 'BucketTemplateUri', description: 'URI for the template that creates Confluences S3 buckets.')
        string(name: 'ConfluenceInstallBinUrl', description: 'URL to Confluence installer-bin.')
        string(name: 'ConfluenceInstallerScriptUrl', description: 'URL to script that installs Confluence to the EC2 instance.')
        string(name: 'ConfluenceListenerCert', description: 'Name/ID of the ACM-managed SSL Certificate to protect public listener.')
        string(name: 'ConfluenceListenPort', description: 'TCP Port number on which the Confluence ELB listens for requests.')
        string(name: 'ConfluenceOsPrepUrl', description: 'URL to script that prepares the EC2 instance for a Confluence install.')
        string(name: 'ConfluenceProxyFqdn', description: 'Fully-qualified domainname of the Confluence reverse-proxy host.')
        string(name: 'ConfluenceServicePort', description: 'TCP Port number that the Confluence host listens to.')
        string(name: 'ConfluenceShareType', description: 'Type of network share hosting shared Confluence content.')
        string(name: 'DbAdminName', description: 'Name of the Confluence master database-user.')
        string(name: 'DbAdminPass', description: 'Password of the Confluence master database-user.')
        string(name: 'DbDataSize', description: 'Size in GiB of the RDS table-space to create.')
        string(name: 'DbInstanceName', description: 'Instance-name of the Confluence database.')
        string(name: 'DbInstanceType', description: 'Amazon RDS instance type')
        string(name: 'DbNodeName', description: 'NodeName of the Confluence database.')
        string(name: 'Domainname', description: 'The domain name')
        string(name: 'Ec2TemplateUri', description: 'URI for the template that creates EC2 instance to host the Confluence Application.')
        string(name: 'EfsTemplateUri', description: 'URI for the template that creates Confluences EFS shares.')
        string(name: 'ElbTemplateUri', description: 'URI for the template that creates ELB proxy providing client-access to the Confluence Application.')
        string(name: 'EpelRepo', defaultValue: 'epel', description: 'An alphanumeric string that represents the EPEL yum repos label')
        string(name: 'HaSubnets', description: 'Select three subnets - each from different Availability Zones.')
        string(name: 'Hostname', description: 'The host name for the EC2 instance')
        string(name: 'IamTemplateUri', description: 'URI for the template that creates Confluences Instance roles.')
        string(name: 'InstanceType', defaultValue: 't2.small', description: 'Amazon EC2 instance type')
        string(name: 'KeyPairName', description: 'Public/private key pairs allow you to securely connect to your instance after it launches')
        string(name: 'NoReboot', defaultValue: 'false', description: 'Controls whether to reboot the instance as the last step of cfn-init execution')
        string(name: 'NoUpdates', defaultValue: 'false', description: 'Controls whether to run yum update during a stack update (on the initial instance launch, SystemPrep _always_ installs updates)')
        string(name: 'PgsqlVersion', description: 'The X.Y.Z version of the PostGreSQL database to deploy.')
        string(name: 'PipRpm', description: 'Name of preferred pip RPM')
        string(name: 'ProvisionUser', defaultValue: 'jenkagent', description: 'Default login user account name')
        string(name: 'ProxyPrettyName', description: 'A short, human-friendly label to assign to the ELB (no capital letters')
        string(name: 'PubElbSubnets', description: 'Select three subnets - each from different, user-facing Availability Zones.')
        string(name: 'PypiIndexUrl', defaultValue: 'https://pypi.org/simple', description: 'URL for pypi')
        string(name: 'PyStache', description: 'Name of preferred pystache RPM')
        string(name: 'RdsTemplateUri', description: 'URI for the template that creates Confluences RDS-hoste PGSQL DB.')
        string(name: 'RolePrefix', description: 'Prefix to apply to IAM role to make things a bit prettier (optional).')
        string(name: 'ServiceTld', description: 'TLD of the IAMable service-name.')
        string(name: 'SgTemplateUri', description: 'URI for the template that creates Confluences SGs.')
        string(name: 'SubnetId', description: 'specific subnet to deploy into')
        string(name: 'TargetVPC', description: 'ID of the VPC to deploy cluster nodes into.')
        string(name: 'WatchmakerConfig', description: '(Optional) URL to a Watchmaker config file')
        string(name: 'WatchmakerEnvironment', description: 'Environment in which the instance is being deployed')
    }

    stages {
        stage ('Prepare Agent Environment') {
            steps {
                deleteDir()
                git branch: "${GitProjBranch}",
                    credentialsId: "${GitCred}",
                    url: "${GitProjUrl}"
                writeFile file: 'ConfluenceDeploy.parms.json',
                    text: /
                    [
                      {
                        "ParameterKey": "AdminPubkeyURL",
                        "ParameterValue": "${env.AdminPubkeyURL}"
                      },
                      {
                        "ParameterKey": "AmiId",
                        "ParameterValue": "${env.AmiId}"
                      },
                      {
                        "ParameterKey": "AppVolumeDevice",
                        "ParameterValue": "${env.AppVolumeDevice}"
                      },
                      {
                        "ParameterKey": "AppVolumeMountPath",
                        "ParameterValue": "${env.AppVolumeMountPath}"
                      },
                      {
                        "ParameterKey": "AppVolumeSize",
                        "ParameterValue": "${env.AppVolumeSize}"
                      },
                      {
                        "ParameterKey": "AppVolumeType",
                        "ParameterValue": "${env.AppVolumeType}"
                      },
                      {
                        "ParameterKey": "BucketTemplateUri",
                        "ParameterValue": "${env.BucketTemplateUri}"
                      },
                      {
                        "ParameterKey": "ConfluenceInstallBinUrl",
                        "ParameterValue": "${env.ConfluenceInstallBinUrl}"
                      },
                      {
                        "ParameterKey": "ConfluenceInstallerScriptUrl",
                        "ParameterValue": "${env.ConfluenceInstallerScriptUrl}"
                      },
                      {
                        "ParameterKey": "ConfluenceListenerCert",
                        "ParameterValue": "${env.ConfluenceListenerCert}"
                      },
                      {
                        "ParameterKey": "ConfluenceListenPort",
                        "ParameterValue": "${env.ConfluenceListenPort}"
                      },
                      {
                        "ParameterKey": "ConfluenceOsPrepUrl",
                        "ParameterValue": "${env.ConfluenceOsPrepUrl}"
                      },
                      {
                        "ParameterKey": "ConfluenceProxyFqdn",
                        "ParameterValue": "${env.ConfluenceProxyFqdn}"
                      },
                      {
                        "ParameterKey": "ConfluenceServicePort",
                        "ParameterValue": "${env.ConfluenceServicePort}"
                      },
                      {
                        "ParameterKey": "ConfluenceShareType",
                        "ParameterValue": "${env.ConfluenceShareType}"
                      },
                      {
                        "ParameterKey": "DbAdminName",
                        "ParameterValue": "${env.DbAdminName}"
                      },
                      {
                        "ParameterKey": "DbAdminPass",
                        "ParameterValue": "${env.DbAdminPass}"
                      },
                      {
                        "ParameterKey": "DbDataSize",
                        "ParameterValue": "${env.DbDataSize}"
                      },
                      {
                        "ParameterKey": "DbInstanceName",
                        "ParameterValue": "${env.DbInstanceName}"
                      },
                      {
                        "ParameterKey": "DbInstanceType",
                        "ParameterValue": "${env.DbInstanceType}"
                      },
                      {
                        "ParameterKey": "DbNodeName",
                        "ParameterValue": "${env.DbNodeName}"
                      },
                      {
                        "ParameterKey": "Domainname",
                        "ParameterValue": "${env.Domainname}"
                      },
                      {
                        "ParameterKey": "Ec2TemplateUri",
                        "ParameterValue": "${env.Ec2TemplateUri}"
                      },
                      {
                        "ParameterKey": "EfsTemplateUri",
                        "ParameterValue": "${env.EfsTemplateUri}"
                      },
                      {
                        "ParameterKey": "ElbTemplateUri",
                        "ParameterValue": "${env.ElbTemplateUri}"
                      },
                      {
                        "ParameterKey": "EpelRepo",
                        "ParameterValue": "${env.EpelRepo}"
                      },
                      {
                        "ParameterKey": "HaSubnets",
                        "ParameterValue": "${env.HaSubnets}"
                      },
                      {
                        "ParameterKey": "Hostname",
                        "ParameterValue": "${env.Hostname}"
                      },
                      {
                        "ParameterKey": "IamTemplateUri",
                        "ParameterValue": "${env.IamTemplateUri}"
                      },
                      {
                        "ParameterKey": "InstanceType",
                        "ParameterValue": "${env.InstanceType}"
                      },
                      {
                        "ParameterKey": "KeyPairName",
                        "ParameterValue": "${env.KeyPairName}"
                      },
                      {
                        "ParameterKey": "NoPublicIp",
                        "ParameterValue": "${env.NoPublicIp}"
                      },
                      {
                        "ParameterKey": "NoReboot",
                        "ParameterValue": "${env.NoReboot}"
                      },
                      {
                        "ParameterKey": "PgsqlVersion",
                        "ParameterValue": "${env.PgsqlVersion}"
                      },
                      {
                        "ParameterKey": "PipRpm",
                        "ParameterValue": "${env.PipRpm}"
                      },
                      {
                        "ParameterKey": "ProvisionUser",
                        "ParameterValue": "${env.ProvisionUser}"
                      },
                      {
                        "ParameterKey": "ProxyPrettyName",
                        "ParameterValue": "${env.ProxyPrettyName}"
                      },
                      {
                        "ParameterKey": "PubElbSubnets",
                        "ParameterValue": "${env.PubElbSubnets}"
                      },
                      {
                        "ParameterKey": "PypiIndexUrl",
                        "ParameterValue": "${env.PypiIndexUrl}"
                      },
                      {
                        "ParameterKey": "PyStache",
                        "ParameterValue": "${env.PyStache}"
                      },
                      {
                        "ParameterKey": "RdsTemplateUri",
                        "ParameterValue": "${env.RdsTemplateUri}"
                      },
                      {
                        "ParameterKey": "RolePrefix",
                        "ParameterValue": "${env.RolePrefix}"
                      },
                      {
                        "ParameterKey": "ServiceTld",
                        "ParameterValue": "${env.ServiceTld}"
                      },
                      {
                        "ParameterKey": "SgTemplateUri",
                        "ParameterValue": "${env.SgTemplateUri}"
                      },
                      {
                        "ParameterKey": "SubnetId",
                        "ParameterValue": "${env.SubnetId}"
                      },
                      {
                        "ParameterKey": "TargetVPC",
                        "ParameterValue": "${env.TargetVPC}"
                      },
                      {
                        "ParameterKey": "WatchmakerConfig",
                        "ParameterValue": "${env.WatchmakerConfig}"
                      },
                      {
                        "ParameterKey": "WatchmakerEnvironment",
                        "ParameterValue": "${env.WatchmakerEnvironment}"
                      }
                    ]
                   /
                }
            }
        stage ('Prepare AWS Environment') {
            options {
                timeout(time: 1, unit: 'HOURS')
            }
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'],
                        sshUserPrivateKey(credentialsId: "${GitCred}", keyFileVariable: 'SSH_KEY_FILE', passphraseVariable: 'SSH_KEY_PASS', usernameVariable: 'SSH_KEY_USER')
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to delete any active ${CfnStackRoot} stacks... "
                        aws --region "${AwsRegion}" cloudformation delete-stack --stack-name "${CfnStackRoot}"

                        sleep 5

                        # Pause if delete is slow
                        while [[ $(
                                    aws cloudformation describe-stacks \
                                      --stack-name ${CfnStackRoot} \
                                      --query 'Stacks[].{Status:StackStatus}' \
                                      --out text 2> /dev/null | \
                                    grep -q DELETE_IN_PROGRESS
                                   )$? -eq 0 ]]
                        do
                           echo "Waiting for stack ${CfnStackRoot} to delete..."
                           sleep 30
                        done
                    '''
                }
            }
        }
        stage ('Launch Confluence Master Stack') {
            options {
                timeout(time: 1, unit: 'HOURS')
            }
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'],
                        sshUserPrivateKey(credentialsId: "${GitCred}", keyFileVariable: 'SSH_KEY_FILE', passphraseVariable: 'SSH_KEY_PASS', usernameVariable: 'SSH_KEY_USER')
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to create stack ${CfnStackRoot}..."
                        aws --region "${AwsRegion}" cloudformation create-stack --stack-name "${CfnStackRoot}" \
                          --disable-rollback --capabilities CAPABILITY_NAMED_IAM \
                          --template-body file://Templates/make_confluence_parent-EFS.tmplt.json \
                          --parameters file://ConfluenceDeploy.parms.json

                        sleep 15

                        # Pause if create is slow
                        while [[ $(
                                    aws cloudformation describe-stacks \
                                      --stack-name ${CfnStackRoot} \
                                      --query 'Stacks[].{Status:StackStatus}' \
                                      --out text 2> /dev/null | \
                                    grep -q CREATE_IN_PROGRESS
                                   )$? -eq 0 ]]
                        do
                           echo "Waiting for stack ${CfnStackRoot} to finish create process..."
                           sleep 30
                        done

                        if [[ $(
                                aws cloudformation describe-stacks \
                                  --stack-name ${CfnStackRoot} \
                                  --query 'Stacks[].{Status:StackStatus}' \
                                  --out text 2> /dev/null | \
                                grep -q CREATE_COMPLETE
                               )$? -eq 0 ]]
                        then
                           echo "Stack-creation successful"
                        else
                           echo "Stack-creation ended with non-successful state"
                           exit 1
                        fi
                    '''
                }
            }
        }
    }
}
