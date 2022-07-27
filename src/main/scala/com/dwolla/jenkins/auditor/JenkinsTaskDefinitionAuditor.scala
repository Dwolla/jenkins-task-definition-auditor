package com.dwolla.jenkins.auditor

import cats.ApplicativeThrow
import cats.effect.Concurrent
import cats.syntax.all._
import feral.lambda.{INothing, LambdaEnv}
import feral.lambda.cloudformation.{CloudFormationCustomResource, CloudFormationCustomResourceRequest, HandlerResponse, PhysicalResourceId, StackId}
import PhysicalResourceIdFromStackId._

case class InvalidPhysicalResourceId(s: StackId) extends RuntimeException(s"Stack ID $s could not be parsed as Physical Resource ID")

class JenkinsTaskDefinitionAuditor[F[_] : Concurrent : LambdaEnv[*[_], CloudFormationCustomResourceRequest[Unit]]](auditTaskDefinitions: AuditTaskDefinitions[F]) extends CloudFormationCustomResource[F, Unit, INothing]{
  private def createOrUpdate(): F[HandlerResponse[INothing]] = {
    LambdaEnv[F, CloudFormationCustomResourceRequest[Unit]]
      .event
      .map(_.StackId)
      .flatMap { stackId =>
        (stackId.asPhysicalResourceId, auditTaskDefinitions.inStack(stackId)).tupled
      }
      .flatMap {
        case (_, Some(incorrect)) => incorrect.raiseError
        case (stackId, None) => HandlerResponse(stackId, None).pure[F]
      }
  }

  override def createResource(input: Unit): F[HandlerResponse[INothing]] =
    createOrUpdate()

  override def updateResource(input: Unit, physicalResourceId: PhysicalResourceId): F[HandlerResponse[INothing]] =
    createOrUpdate()

  override def deleteResource(input: Unit, physicalResourceId: PhysicalResourceId): F[HandlerResponse[INothing]] =
    LambdaEnv[F, CloudFormationCustomResourceRequest[Unit]]
      .event
      .flatMap(_.StackId.asPhysicalResourceId)
      .map(HandlerResponse(_, None))
}

object JenkinsTaskDefinitionAuditor {
  def apply[F[_] : Concurrent : LambdaEnv[*[_], CloudFormationCustomResourceRequest[Unit]]](auditTaskDefinitions: AuditTaskDefinitions[F]) =
    new JenkinsTaskDefinitionAuditor(auditTaskDefinitions)
}

final class PhysicalResourceIdFromStackId(val stackId: StackId) extends AnyVal {
  def asPhysicalResourceId[F[_] : ApplicativeThrow]: F[PhysicalResourceId] =
    PhysicalResourceId(stackId.value).liftTo[F](InvalidPhysicalResourceId(stackId))
}

object PhysicalResourceIdFromStackId {
  implicit def toPhysicalResourceIdFromStackId(stackId: StackId): PhysicalResourceIdFromStackId = new PhysicalResourceIdFromStackId(stackId)
}
