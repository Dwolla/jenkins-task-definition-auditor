package com.dwolla.jenkins

import cats.Show
import monix.newtypes._
import software.amazon.awssdk.services.ecs.model.TaskDefinition

package object auditor {
  type TaskDefinitionArn = TaskDefinitionArn.Type
  object TaskDefinitionArn extends NewtypeWrapped[String]

  implicit val showTaskDefinition: Show[TaskDefinition] = Show.show(_.taskDefinitionArn().split('/')(1))
}
