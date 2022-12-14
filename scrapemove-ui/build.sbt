val Http4sVersion    = "1.0.0-M4"
val elastic4sVersion = "8.4.3"
val circeVersion     = "0.14.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.hunorkovacs",
    name := "zio-http4s-try",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-blaze-server"     % Http4sVersion,
      "org.http4s"             %% "http4s-dsl"              % Http4sVersion,
      "org.http4s"             %% "http4s-scalatags"        % Http4sVersion,
      "com.lihaoyi"            %% "scalatags"               % "0.12.0",
      "dev.zio"                %% "zio"                     % "1.0.17",
      "dev.zio"                %% "zio-interop-cats"        % "22.0.0.0",
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-effect-zio"    % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-json-circe"    % elastic4sVersion,
      "org.typelevel"          %% "cats-core"               % "2.9.0"
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic", // Automatic schema derivation
      "io.circe" %% "circe-generic-extras", // Snake case decoding
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
  "-Ymacro-annotations"
)
