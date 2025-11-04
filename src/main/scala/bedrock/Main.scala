package bedrock

import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest

@main def askBedrock(): Unit =
  // Create credentials provider using the developerPlayground profile
  val credentialsProvider = ProfileCredentialsProvider.builder()
    .profileName("developerPlayground")
    .build()

  // Create Bedrock Runtime client
  val bedrockClient = BedrockRuntimeClient.builder()
    .region(Region.US_EAST_1)
    .credentialsProvider(credentialsProvider)
    .build()

  try
    // Prepare the request payload for Claude 3 Sonnet
    val prompt = "Generate a Mermaid flowchart that shows the processes involved in the water lifecycle of Earth."

    val objectMapper = new ObjectMapper()
    val requestBody = objectMapper.createObjectNode()

    // Build the messages array for Claude 3
    val messagesArray = objectMapper.createArrayNode()
    val messageNode = objectMapper.createObjectNode()
    messageNode.put("role", "user")
    messageNode.put("content", prompt)
    messagesArray.add(messageNode)

    requestBody.set("messages", messagesArray)
    requestBody.put("max_tokens", 2048)
    requestBody.put("anthropic_version", "bedrock-2023-05-31")

    val requestBodyJson = objectMapper.writeValueAsString(requestBody)

    println("Asking Bedrock: " + prompt)
    println("=" * 60)

    // Invoke the model
    val modelId = "anthropic.claude-3-sonnet-20240229-v1:0"
    val request = InvokeModelRequest.builder()
      .modelId(modelId)
      .body(SdkBytes.fromUtf8String(requestBodyJson))
      .build()

    val response = bedrockClient.invokeModel(request)

    // Parse the response
    val responseBody = response.body().asUtf8String()
    val responseJson = objectMapper.readTree(responseBody)

    // Extract the content from the response
    val content = responseJson.get("content").get(0).get("text").asText()

    println(content)
    println("=" * 60)

  catch
    case e: Exception =>
      println(s"Error calling Bedrock: ${e.getMessage}")
      e.printStackTrace()
  finally
    bedrockClient.close()
