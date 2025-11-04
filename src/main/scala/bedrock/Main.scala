package bedrock

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.UserCredentials
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import scala.io.Source
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration

@main def askBedrock(): Unit =
  // Create credentials provider using the developerPlayground profile
  val credentialsProvider = ProfileCredentialsProvider.builder()
    .profileName("developerPlayground")
    .build()

  sys.exit()

  // Configure HTTP client with extended timeouts for Bedrock operations
  val httpClient = ApacheHttpClient.builder()
    .socketTimeout(Duration.ofMinutes(5))
    .connectionTimeout(Duration.ofSeconds(60))
    .build()

  val clientConfig = ClientOverrideConfiguration.builder()
    .apiCallTimeout(Duration.ofMinutes(5))
    .apiCallAttemptTimeout(Duration.ofMinutes(5))
    .build()

  // Create Bedrock Runtime client with custom timeouts
  val bedrockClient = BedrockRuntimeClient.builder()
    .region(Region.US_EAST_1)
    .credentialsProvider(credentialsProvider)
    .httpClient(httpClient)
    .overrideConfiguration(clientConfig)
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

    // Read the prompt template from file
    val promptTemplate = readPromptTemplate("prompt.txt")

    // Replace the placeholder with actual runbook content
    val prompt = promptTemplate.replace("{RUNBOOK_CONTENT}", runbookContent)


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

    // Generate markdown file with diagram and link to source
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val outputFileName = s"runbook-diagram-$timestamp.md"

    val markdownContent = s"""# Runbook Diagram
      |
      |Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
      |
      |## Source Document
      |
      |[View Original Runbook]($docUrl)
      |
      |## Decision-Making Diagram
      |
      |$content
      |""".stripMargin

    Files.write(
      Paths.get(outputFileName),
      markdownContent.getBytes("UTF-8"),
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )

    println(s"Markdown file saved to: $outputFileName")
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

// Helper function to read prompt template from file
def readPromptTemplate(filePath: String): String =
  val source = Source.fromFile(filePath)
  try source.mkString finally source.close()

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
