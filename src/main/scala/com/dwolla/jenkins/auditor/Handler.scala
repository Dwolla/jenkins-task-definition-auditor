package com.dwolla.jenkins.auditor

import cats.data._
import cats.effect.std.Random
import cats.effect.{Trace => _, _}
import cats.tagless.syntax.all._
import com.dwolla.tracing._
import feral.lambda.cloudformation._
import feral.lambda.{INothing, IOLambda, KernelSource, LambdaEnv, TracedHandler}
import natchez._
import natchez.http4s.NatchezMiddleware
import natchez.xray.{XRay, XRayEnvironment}
import org.http4s.client.{Client, middleware}
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j._

class Handler extends IOLambda[CloudFormationCustomResourceRequest[Unit], INothing] {
  private def resources[F[_] : Async]: Resource[F, LambdaEnv[F, CloudFormationCustomResourceRequest[Unit]] => F[Option[INothing]]] =
    for {
      implicit0(random: Random[F]) <- Resource.eval(Random.scalaUtilRandom[F])
      client <- httpClient[F]
      entryPoint <- XRayEnvironment[Resource[F, *]].daemonAddress.flatMap {
        case Some(addr) => XRay.entryPoint(addr)
        case None => XRay.entryPoint[F]()
      }
      cloudformation <- CloudFormationAlg[F].map(_.mapK(Kleisli.liftK[F, Span[F]]))
      ecs <- EcsAlg[F].map(_.mapK(Kleisli.liftK[F, Span[F]]).withTracing)
      auditTaskDefinitions = AuditTaskDefinitions(cloudformation, ecs)
    } yield { implicit env: LambdaEnv[F, CloudFormationCustomResourceRequest[Unit]] =>
      TracedHandler(entryPoint, Kleisli { (span: Span[F]) =>
        CloudFormationCustomResource(tracedHttpClient(client, span), JenkinsTaskDefinitionAuditor(auditTaskDefinitions)).run(span)
      })
    }

  private def httpClient[F[_] : Async]: Resource[F, Client[F]] =
    EmberClientBuilder
      .default[F]
      .build
      .map(middleware.Logger[F](logHeaders = true, logBody = true))

  private def tracedHttpClient[F[_] : MonadCancelThrow](client: Client[F], span: Span[F]): Client[Kleisli[F, Span[F], *]] =
    NatchezMiddleware.client(client.translate(Kleisli.liftK[F, Span[F]])(Kleisli.applyK(span)))

  override def handler: Resource[IO, LambdaEnv[IO, CloudFormationCustomResourceRequest[Unit]] => IO[Option[INothing]]] =
    resources[IO]

  /**
   * The XRay kernel comes from environment variables, so we don't need to extract anything from the incoming event
   */
  private implicit def kernelSource[Event]: KernelSource[Event] = KernelSource.emptyKernelSource
}
