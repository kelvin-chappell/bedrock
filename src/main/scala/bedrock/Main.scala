package bedrock

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.UserCredentials
import io.github.cdimascio.dotenv.Dotenv
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region.US_EAST_1
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}
import scala.io.Source

@main def askBedrock(docUrl: String): Unit =
  val dotenv = Dotenv.configure().load()

  val credentialsProvider = ProfileCredentialsProvider.builder()
    .profileName("developerPlayground")
    .build()

  // Configure HTTP client with extended timeouts for Bedrock operations
  val httpClient = ApacheHttpClient.builder()
    .socketTimeout(Duration.ofMinutes(5))
    .connectionTimeout(Duration.ofMinutes(1))
    .build()

  // Create Bedrock Runtime client with custom timeouts
  val clientConfig = ClientOverrideConfiguration.builder()
    .apiCallTimeout(Duration.ofMinutes(5))
    .apiCallAttemptTimeout(Duration.ofMinutes(5))
    .build()

  val bedrockClient = BedrockRuntimeClient.builder()
    .region(US_EAST_1)
    .credentialsProvider(credentialsProvider)
    .httpClient(httpClient)
    .overrideConfiguration(clientConfig)
    .build()

  try

    val clientId = Option(dotenv.get("GOOGLE_CLIENT_ID"))
      .getOrElse(throw new RuntimeException("GOOGLE_CLIENT_ID not set in .env file or environment"))

    val clientSecret = Option(dotenv.get("GOOGLE_CLIENT_SECRET"))
      .getOrElse(throw new RuntimeException("GOOGLE_CLIENT_SECRET not set in .env file or environment"))

    val refreshToken = Option(dotenv.get("GOOGLE_REFRESH_TOKEN"))
      .getOrElse(throw new RuntimeException("GOOGLE_REFRESH_TOKEN not set in .env file or environment"))

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

    val document = docsService.documents().get(docId).execute()

    val runbookContent = extractTextFromDocument(document)

    println(s"Successfully read ${runbookContent.length} characters from Google Doc")
    println("=" * 60)

    val promptTemplate = readPromptTemplate("prompt.txt")

    val prompt = promptTemplate.replace("{RUNBOOK_CONTENT}", runbookContent)

    val objectMapper = new ObjectMapper()
    val requestBody = objectMapper.createObjectNode()

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

    val modelId = "anthropic.claude-3-5-sonnet-20240620-v1:0"
    val request = InvokeModelRequest.builder()
      .modelId(modelId)
      .body(SdkBytes.fromUtf8String(requestBodyJson))
      .build()

    val response = bedrockClient.invokeModel(request)

    val responseBody = response.body().asUtf8String()
    val responseJson = objectMapper.readTree(responseBody)

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

def readPromptTemplate(filePath: String): String =
  val source = Source.fromFile(filePath)
  try source.mkString finally source.close()

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
