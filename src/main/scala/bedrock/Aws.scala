package bedrock

import bedrock.Config.config
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient

object Aws:
  private val credentialsProvider = ProfileCredentialsProvider.builder()
    .profileName(config.aws.profileName)
    .build()

  private val httpClient = ApacheHttpClient.builder()
    .socketTimeout(config.aws.httpClient.socketTimeout)
    .connectionTimeout(config.aws.httpClient.connectionTimeout)
    .build()

  private val clientConfig = ClientOverrideConfiguration.builder()
    .apiCallTimeout(config.aws.clientOverride.apiCallTimeout)
    .apiCallAttemptTimeout(config.aws.clientOverride.apiCallAttemptTimeout)
    .build()

  val bedrockClient: BedrockRuntimeClient = BedrockRuntimeClient.builder()
    .region(Region.of(config.aws.region))
    .credentialsProvider(credentialsProvider)
    .httpClient(httpClient)
    .overrideConfiguration(clientConfig)
    .build()
