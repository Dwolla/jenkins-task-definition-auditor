package com.dwolla.jenkins

import cats.Show
import cats.data.NonEmptyList
import fs2.Collector
import monix.newtypes._
import software.amazon.awssdk.services.ecs.model.TaskDefinition

package object auditor {
  type TaskDefinitionArn = TaskDefinitionArn.Type
  object TaskDefinitionArn extends NewtypeWrapped[String]

  implicit val showTaskDefinition: Show[TaskDefinition] = Show.show(_.taskDefinitionArn().split('/')(1))

  def OptionalNonEmptyList[A]: Collector.Aux[A, Option[NonEmptyList[A]]] = new Collector[A] {
    override type Out = Option[NonEmptyList[A]]
    override def newBuilder: Collector.Builder[A, Option[NonEmptyList[A]]] =
      Collector.Builder.fromIterableFactory(List).mapResult(NonEmptyList.fromList)
  }
}
