# jenkins-task-definition-auditor

CloudFormation custom resource to audit the task definitions created for Jenkins, to make sure they're compatible with the ECS Cloud plugin

The ECS Cloud plugin assumes that the first container listed in the task definition for a given label will be the one that should be given Jenkins config information. This isn't always true when the task definitions are created by CloudFormation, because you don't really have control over the order of the containers in the task definition.

If any of the task definitions have containers that are out of order, this resource will signal failure, causing the stack to roll back.
