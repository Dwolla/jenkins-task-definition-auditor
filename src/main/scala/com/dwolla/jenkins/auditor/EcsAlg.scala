package com.dwolla.jenkins.auditor

import cats.effect.{Trace => _, _}
import cats.tagless.Derive
import cats.tagless.aop.Instrument
import com.dwolla.fs2aws.AwsEval
import software.amazon.awssdk.services.ecs.EcsAsyncClient
import software.amazon.awssdk.services.ecs.model.{DescribeTaskDefinitionRequest, TaskDefinition}

trait EcsAlg[F[_]] {
  def taskDefinition(arn: TaskDefinitionArn): F[TaskDefinition]
}

object EcsAlg {
  implicit val EcsAlgInstrument: Instrument[EcsAlg] = Derive.instrument

  def apply[F[_] : Async]: Resource[F, EcsAlg[F]] =
    Resource.fromAutoCloseable(Sync[F].delay(EcsAsyncClient.builder().build()))
      .map { client =>
        new EcsAlg[F] {
          override def taskDefinition(arn: TaskDefinitionArn): F[TaskDefinition] =
            AwsEval.eval[F](DescribeTaskDefinitionRequest.builder().taskDefinition(arn.value).build())(client.describeTaskDefinition) {
              _.taskDefinition()
            }
        }
      }
}
