# Serverless - Lambda Function for Notification Emails

This lambda function is triggered by new AWS SNS message to send email to the users using AWS SES. 

### Installation

> Pre-requisites
> * Java 11
> * Maven
> * AWS CLI with configured credentials

### To Deploy the Lambda Locally
1. Clone this repository to your local machine.
2. <code>$ cd serverless</code>
3. <code>$ mvn clean install</code> to build the project.
4. <code>$ zip serverless.zip target/serverless-0.0.1-SNAPSHOT.jar </code> to zip the execution file.
5. In AWS CLI, <code>$ aws s3 cp serverless.zip s3://{your S3 bucket name}</code> to upload the zip to your S3 bucket.
6. In AWS CLI, type the following command to deploy the lambda function.
    <code>$ aws lambda update-function-code \
          --function-name "EmailNotification" \
          --s3-bucket {your S3 bucket name} \
          --s3-key serverless.zip
    </code>