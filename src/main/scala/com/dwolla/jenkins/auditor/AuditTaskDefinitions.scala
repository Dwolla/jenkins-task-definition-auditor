package com.dwolla.jenkins.auditor

import cats.effect.{Trace => _, _}
import cats.syntax.all._
import feral.lambda.cloudformation.StackId
import mouse.all._
import org.typelevel.log4cats.{Logger, LoggerFactory}
import software.amazon.awssdk.services.ecs.model.TaskDefinition

import scala.jdk.CollectionConverters._

trait AuditTaskDefinitions[F[_]] {
  def inStack(stack: StackId): F[Option[InvalidTaskDefinitions]]
}

object AuditTaskDefinitions {
  def apply[F[_] : Concurrent : LoggerFactory](cloudformation: CloudFormationAlg[F],
                                               ecs: EcsAlg[F]): AuditTaskDefinitions[F] =
    new AuditTaskDefinitions[F] {
      override def inStack(stack: StackId): F[Option[InvalidTaskDefinitions]] = {
        LoggerFactory[F].create.flatMap { implicit logger =>
          cloudformation.taskDefinitionsOfStack(stack)
            .evalMap(ecs.taskDefinition)
            .filter(_.hasContainerDefinitions)
            .filterNot(firstContainerIsDwollaJenkinsAgent)
            .evalTap { td =>
              Logger[F].warn(
                s"""🔥 Task Definition ${td.taskDefinitionArn()} has containers with the following images:
                   |${td.containerDefinitions().asScala.toList.map(_.image()).mkString_(" - ", "\n - ", "")}
                   |
                   |""".stripMargin
              )
            }
            .compile
            .to(OptionalNonEmptyList)
            .liftOptionT
            .semiflatTap { nel =>
              Logger[F].error(s"☄️ Whelp, there are ${nel.size} tasks with containers in the wrong order.")
            }
            .map(InvalidTaskDefinitions)
            .value
        }
      }
    }

  private val firstContainerIsDwollaJenkinsAgent: TaskDefinition => Boolean =
    _.containerDefinitions().asScala
      .headOption
      .exists(_.image().contains("dwolla/jenkins-agent-"))
}
