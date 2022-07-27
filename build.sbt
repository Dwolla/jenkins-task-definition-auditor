ThisBuild / organization := "com.dwolla"
ThisBuild / description := "CloudFormation custom resource to audit the task definitions created for Jenkins, to make sure they're compatible with the ECS Cloud plugin"
ThisBuild / homepage := Some(url("https://github.com/Dwolla/jenkins-task-definition-auditor"))
ThisBuild / licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / developers := List(
  Developer(
    "bpholt",
    "Brian Holt",
    "bholt+postgres-init-custom-resource@dwolla.com",
    url("https://dwolla.com")
  ),
)
ThisBuild / startYear := Option(2022)
ThisBuild / libraryDependencies ++= Seq(
  compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
)

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty
ThisBuild / githubWorkflowPublish := Seq.empty

lazy val `jenkins-task-definition-auditor` = (project in file("."))
  .settings(
    maintainer := developers.value.head.email,
    topLevelDirectory := None,

    libraryDependencies ++= {
      val natchezVersion = "0.1.6"
      val feralVersion = "0.1.0-M13"

      Seq(
        "org.typelevel" %% "feral-lambda-cloudformation-custom-resource" % feralVersion,
        "org.tpolecat" %% "natchez-xray" % natchezVersion,
        "org.tpolecat" %% "natchez-http4s" % "0.3.2",
        "org.typelevel" %% "cats-tagless-macros" % "0.14.0",
        "org.typelevel" %% "mouse" % "1.1.0",
        "io.monix" %% "newtypes-core" % "0.2.3",
        "io.circe" %% "circe-generic" % "0.14.2",
        "org.http4s" %% "http4s-ember-client" % "0.23.13",
        "org.typelevel" %% "log4cats-slf4j" % "2.3.2",
        "com.amazonaws" % "aws-lambda-java-log4j2" % "1.5.1",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.18.0",
        "com.chuusai" %% "shapeless" % "2.3.9",
        "com.dwolla" %% "fs2-aws-java-sdk2" % "3.0.0-RC1",
        "software.amazon.awssdk" % "cloudformation" % "2.17.229",
        "software.amazon.awssdk" % "ecs" % "2.17.229",
      )
    },
  )
  .enablePlugins(UniversalPlugin, JavaAppPackaging)

lazy val serverlessDeployCommand = settingKey[Seq[String]]("serverless command to deploy the application")
serverlessDeployCommand := "serverless deploy --verbose".split(' ').toSeq

lazy val deploy = inputKey[Int]("deploy to AWS")
deploy := Def.inputTask {
  import scala.sys.process.Process

  val commandParts = serverlessDeployCommand.value ++ Seq("--stage", Stage.parser.parsed.name)
  streams.value.log.log(Level.Info, commandParts.mkString(" "))

  val exitCode = Process(
    commandParts,
    Option((`jenkins-task-definition-auditor` / baseDirectory).value),
    "DATABASE_ARTIFACT_PATH" -> (`jenkins-task-definition-auditor` / Universal / packageBin).value.toString,
  ).!

  if (exitCode == 0) exitCode
  else throw new IllegalStateException("Serverless returned a non-zero exit code. Please check the logs for more information.")
}.evaluated
