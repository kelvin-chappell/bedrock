package bedrock

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region.US_EAST_1
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient

import java.time.Duration

object Aws:  
  private val credentialsProvider = ProfileCredentialsProvider.builder()
    .profileName("developerPlayground")
    .build()

  private val httpClient = ApacheHttpClient.builder()
    .socketTimeout(Duration.ofMinutes(5))
    .connectionTimeout(Duration.ofMinutes(1))
    .build()

  private val clientConfig = ClientOverrideConfiguration.builder()
    .apiCallTimeout(Duration.ofMinutes(5))
    .apiCallAttemptTimeout(Duration.ofMinutes(5))
    .build()

  val bedrockClient: BedrockRuntimeClient = BedrockRuntimeClient.builder()
    .region(US_EAST_1)
    .credentialsProvider(credentialsProvider)
    .httpClient(httpClient)
    .overrideConfiguration(clientConfig)
    .build()
