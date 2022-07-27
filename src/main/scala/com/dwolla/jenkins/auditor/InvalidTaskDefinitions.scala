package com.dwolla.jenkins.auditor

import cats.data._
import cats.syntax.all._
import software.amazon.awssdk.services.ecs.model.TaskDefinition

case class InvalidTaskDefinitions(defs: NonEmptyList[TaskDefinition]) extends RuntimeException {
  override def getMessage: String =
    s"""The following ${defs.size} task definitions contain container definitions where the "dwolla/jenkins-agent-*" container is not listed first:
       |
       |${defs.mkString_(" - ", "\n - ", "")}
       |""".stripMargin
}
