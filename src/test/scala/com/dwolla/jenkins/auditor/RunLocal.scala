package com.dwolla.jenkins.auditor

import cats.effect._
import feral.lambda.cloudformation.CloudFormationRequestType.CreateRequest
import feral.lambda.cloudformation._
import feral.lambda.{LambdaEnv, TestContext}
import org.http4s.syntax.all._

object RunLocal extends IOApp.Simple {
  private val req = CloudFormationCustomResourceRequest(
    CreateRequest,
    uri"https://webhook.site/redacted", // TODO change me
    StackId("arn:aws:cloudformation:us-west-2:{account}:stack/{stack name / id}"), // TODO change me
    RequestId(""),
    ResourceType("Custom::JenkinsTaskDefinitionAuditor"),
    LogicalResourceId("JenkinsTaskDefinitionAuditor"),
    None,
    (),
    None
  )

  override def run: IO[Unit] =
    new Handler()
      .handler
      .evalMap(_(LambdaEnv.pure(req, TestContext[IO])))
      .use_
}
