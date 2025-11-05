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
      "software.amazon.awssdk" % "apache-client" % "2.28.16",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10",
      "com.google.api-client" % "google-api-client" % "2.8.1",
      "com.google.apis" % "google-api-services-docs" % "v1-rev20220609-2.0.0",
      "com.google.auth" % "google-auth-library-oauth2-http" % "1.19.0",
      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.39.0",
      "org.slf4j" % "slf4j-simple" % "2.0.9",
      "io.github.cdimascio" % "dotenv-java" % "3.0.0",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.8",
      "com.typesafe" % "config" % "1.4.2"
    )
  )
