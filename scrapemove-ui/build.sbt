val Http4sVersion = "1.0.0-M4"

lazy val root = (project in file("."))
  .settings(
    organization := "com.hunorkovacs",
    name := "zio-http4s-try",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      "org.http4s"  %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"  %% "http4s-dsl"          % Http4sVersion,
      "org.http4s"  %% "http4s-scalatags"    % Http4sVersion,
      "com.lihaoyi" %% "scalatags"           % "0.12.0",
      "dev.zio"     %% "zio"                 % "1.0.17",
      "dev.zio"     %% "zio-interop-cats"    % "22.0.0.0"
    )
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings"
)
