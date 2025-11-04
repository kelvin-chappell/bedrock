lazy val root = project
  .in(file("."))
  .settings(
    name := "bedrock",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.7.3",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "software.amazon.awssdk" % "bedrockruntime" % "2.28.16",
      "software.amazon.awssdk" % "auth" % "2.28.16",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.18.1"
    )
  )
