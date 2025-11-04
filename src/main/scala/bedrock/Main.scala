package bedrock

import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import scala.io.Source

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
    // Read the runbook.txt file
    val runbookPath = "runbook.txt"
    val runbookSource = Source.fromFile(runbookPath)
    val runbookContent = try runbookSource.mkString finally runbookSource.close()

    // Prepare the request payload for Claude 3 Sonnet
    val prompt = s"""Based on the following Identity 24/7 Runbook, create a Mermaid diagram that shows the decision-making process for a support engineer responding to an incident.

The diagram should:
1. Show what observations/symptoms to look for
2. Guide the engineer through the decision-making process based on those observations
3. Show what remediation actions to take
4. Use the most appropriate Mermaid diagram type for this decision-making process (e.g., flowchart, state diagram, etc.)

Focus on the key decision points and actions that a support engineer would need to make during an incident.

IMPORTANT SYNTAX REQUIREMENTS:
- Ensure the Mermaid diagram syntax is completely valid and will render without errors
- Do NOT use parentheses () in node labels - use square brackets, quotes, or other descriptors instead
- Avoid special characters that might break Mermaid parsing (parentheses, semicolons, etc.)
- Use proper Mermaid flowchart syntax with valid node definitions
- Test that all node IDs are unique and properly referenced
- Use HTML entities or escape sequences if special characters are absolutely necessary

Here is the runbook content:

$runbookContent

Please generate only the Mermaid diagram code without additional explanation. Ensure the syntax is 100% valid and will render in a markdown file without parse errors."""

    val objectMapper = new ObjectMapper()
    val requestBody = objectMapper.createObjectNode()

    // Build the messages array for Claude 3
    val messagesArray = objectMapper.createArrayNode()
    val messageNode = objectMapper.createObjectNode()
    messageNode.put("role", "user")
    messageNode.put("content", prompt)
    messagesArray.add(messageNode)

    requestBody.set("messages", messagesArray)
    requestBody.put("max_tokens", 4096)
    requestBody.put("anthropic_version", "bedrock-2023-05-31")

    val requestBodyJson = objectMapper.writeValueAsString(requestBody)

    println("Generating Mermaid diagram from Identity Runbook...")
    println("=" * 60)

    // Invoke the model
    val modelId = "anthropic.claude-3-5-sonnet-20240620-v1:0"
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
