package feral.lambda

import cats._
import cats.syntax.all._

import scala.concurrent.duration._

object TestContext {
  def apply[F[_] : Applicative]: Context[F] =
    new Context[F](
      "jenkins-task-definition-auditor",
      "0.0.1",
      "",
      2048,
      "",
      "",
      "",
      None,
      None,
      42.hours.pure[F]
    )
}
