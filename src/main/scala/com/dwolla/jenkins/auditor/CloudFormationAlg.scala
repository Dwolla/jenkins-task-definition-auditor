package com.dwolla.jenkins.auditor

import cats.effect.{Trace => _, _}
import cats.tagless.FunctorK
import cats.~>
import com.dwolla.fs2aws.AwsEval
import feral.lambda.cloudformation.StackId
import fs2.Stream
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest

trait CloudFormationAlg[F[_]] {
  def taskDefinitionsOfStack(stack: StackId): Stream[F, TaskDefinitionArn]
}

object CloudFormationAlg {
  implicit val CloudFormationAlgFunctorK: FunctorK[CloudFormationAlg] = new FunctorK[CloudFormationAlg] {
    override def mapK[F[_], G[_]](af: CloudFormationAlg[F])(fk: F ~> G): CloudFormationAlg[G] = new CloudFormationAlg[G] {
      override def taskDefinitionsOfStack(stack: StackId): Stream[G, TaskDefinitionArn] = af.taskDefinitionsOfStack(stack).translate(fk)
    }
  }

  def apply[F[_] : Async]: Resource[F, CloudFormationAlg[F]] =
    Resource.fromAutoCloseable(Sync[F].delay(CloudFormationAsyncClient.builder().build()))
      .map { client =>
        new CloudFormationAlg[F] {
          override def taskDefinitionsOfStack(stack: StackId): Stream[F, TaskDefinitionArn] =
            AwsEval.unfold[F](client.listStackResourcesPaginator(ListStackResourcesRequest.builder().stackName(stack.value).build()))(_.stackResourceSummaries())
              .filter(_.resourceType() == "AWS::ECS::TaskDefinition")
              .map(_.physicalResourceId())
              .map(TaskDefinitionArn(_))
        }
      }
}
