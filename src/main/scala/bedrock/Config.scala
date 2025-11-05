package bedrock

import pureconfig.ConfigSource
import pureconfig.ConfigReader
import io.github.cdimascio.dotenv.Dotenv
import java.time.Duration

// Custom Duration reader for HOCON durations
given ConfigReader[Duration] = ConfigReader.fromString { str =>
  import pureconfig.error.CannotConvert
  try {
    import com.typesafe.config.ConfigFactory
    val config = ConfigFactory.parseString(s"duration = $str")
    val javaDuration = config.getDuration("duration")
    Right(javaDuration)
  } catch {
    case e: Exception =>
      Left(CannotConvert(str, "Duration", e.getMessage))
  }
}

case class AwsHttpClientConfig(
  socketTimeout: Duration,
  connectionTimeout: Duration
) derives ConfigReader

case class AwsClientOverrideConfig(
  apiCallTimeout: Duration,
  apiCallAttemptTimeout: Duration
) derives ConfigReader

case class AwsModelConfig(
  id: String,
  maxTokens: Int,
  anthropicVersion: String
) derives ConfigReader

case class AwsConfig(
  profileName: String,
  region: String,
  httpClient: AwsHttpClientConfig,
  clientOverride: AwsClientOverrideConfig,
  model: AwsModelConfig
) derives ConfigReader

case class GoogleConfig(
  clientId: String,
  clientSecret: String,
  refreshToken: String,
  applicationName: String
) derives ConfigReader

case class AppConfig(
  promptFile: String,
  outputTemplateFile: String
) derives ConfigReader

case class BedrockConfig(
  aws: AwsConfig,
  google: GoogleConfig,
  app: AppConfig
) derives ConfigReader

object Config:
  // Load .env file first to populate environment variables
  private val dotenv = try {
    Dotenv.configure()
      .ignoreIfMissing()
      .load()
  } catch {
    case _: Exception => null
  }

  // Set environment variables from .env file if it exists
  if (dotenv != null) {
    dotenv.entries().forEach { entry =>
      if (System.getenv(entry.getKey) == null) {
        // Only set if not already set in the environment
        System.setProperty(entry.getKey, entry.getValue)
      }
    }
  }

  // Load configuration using PureConfig
  val config: BedrockConfig = ConfigSource.default
    .at("bedrock")
    .loadOrThrow[BedrockConfig]
