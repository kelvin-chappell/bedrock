package bedrock

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.UserCredentials
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
    // Read Google Doc URL and credentials from environment variables
    val docUrl = sys.env.getOrElse("GOOGLE_DOC_URL",
      throw new RuntimeException("GOOGLE_DOC_URL environment variable not set"))

    val clientId = sys.env.getOrElse("GOOGLE_CLIENT_ID",
      throw new RuntimeException("GOOGLE_CLIENT_ID environment variable not set"))

    val clientSecret = sys.env.getOrElse("GOOGLE_CLIENT_SECRET",
      throw new RuntimeException("GOOGLE_CLIENT_SECRET environment variable not set"))

    val refreshToken = sys.env.getOrElse("GOOGLE_REFRESH_TOKEN",
      throw new RuntimeException("GOOGLE_REFRESH_TOKEN environment variable not set"))

    // Extract document ID from URL
    // Google Docs URLs typically look like: https://docs.google.com/document/d/{DOCUMENT_ID}/edit
    val docId = extractDocumentId(docUrl)

    println(s"Reading runbook from Google Doc: $docId")

    // Initialize Google Docs API client with user credentials
    val credentials = UserCredentials.newBuilder()
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setRefreshToken(refreshToken)
      .build()

    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val jsonFactory = GsonFactory.getDefaultInstance

    val docsService = new Docs.Builder(
      httpTransport,
      jsonFactory,
      new HttpCredentialsAdapter(credentials)
    )
      .setApplicationName("Bedrock Runbook Reader")
      .build()

    // Fetch the document
    val document = docsService.documents().get(docId).execute()

    // Extract text content from the document
    val runbookContent = extractTextFromDocument(document)

    println(s"Successfully read ${runbookContent.length} characters from Google Doc")
    println("=" * 60)

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

    println("Generating Mermaid diagram from runbook...")
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

// Helper function to extract document ID from Google Docs URL
def extractDocumentId(url: String): String =
  // Handle different Google Docs URL formats
  val patterns = List(
    """https://docs\.google\.com/document/d/([a-zA-Z0-9-_]+)""".r,
    """https://docs\.google\.com/document/u/\d+/d/([a-zA-Z0-9-_]+)""".r
  )

  patterns.flatMap(_.findFirstMatchIn(url)).headOption match
    case Some(m) => m.group(1)
    case None =>
      // If URL doesn't match, assume it's already a document ID
      if url.matches("[a-zA-Z0-9-_]+") then url
      else throw new IllegalArgumentException(s"Invalid Google Docs URL or document ID: $url")

// Helper function to extract text content from Google Docs Document
def extractTextFromDocument(document: com.google.api.services.docs.v1.model.Document): String =
  import scala.jdk.CollectionConverters.*

  val content = document.getBody.getContent
  if content == null then return ""

  val textBuilder = new StringBuilder()

  content.asScala.foreach { structuralElement =>
    if structuralElement.getParagraph != null then
      val paragraph = structuralElement.getParagraph
      val elements = paragraph.getElements

      if elements != null then
        elements.asScala.foreach { element =>
          val textRun = element.getTextRun
          if textRun != null && textRun.getContent != null then
            textBuilder.append(textRun.getContent)
        }
  }

  textBuilder.toString
