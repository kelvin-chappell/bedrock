package bedrock

import bedrock.Aws.bedrockClient
import bedrock.Google.docsService
import io.circe.*
import io.circe.parser.*
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.io.Source

@main def askBedrock(docUrl: String): Unit =
  try
    // Extract document ID from URL
    // Google Docs URLs typically look like: https://docs.google.com/document/d/{DOCUMENT_ID}/edit
    val docId = extractDocumentId(docUrl)

    println(s"Reading runbook from Google Doc: $docId")

    val document = docsService.documents().get(docId).execute()

    val runbookContent = extractTextFromDocument(document)

    println(s"Successfully read ${runbookContent.length} characters from Google Doc")
    println("=" * 60)

    val promptTemplate = readPromptTemplate("prompt.txt")

    val prompt = promptTemplate.replace("{RUNBOOK_CONTENT}", runbookContent)

    val requestBody = Json.obj(
      "messages" -> Json.arr(
        Json.obj(
          "role" -> Json.fromString("user"),
          "content" -> Json.fromString(prompt)
        )
      ),
      "max_tokens" -> Json.fromInt(4096),
      "anthropic_version" -> Json.fromString("bedrock-2023-05-31")
    )

    val requestBodyJson = requestBody.noSpaces

    println("Generating Mermaid diagram from runbook...")
    println("=" * 60)

    val modelId = "anthropic.claude-3-5-sonnet-20240620-v1:0"
    val request = InvokeModelRequest.builder()
      .modelId(modelId)
      .body(SdkBytes.fromUtf8String(requestBodyJson))
      .build()

    val response = bedrockClient.invokeModel(request)

    val responseBody = response.body().asUtf8String()

    val content = parse(responseBody).flatMap { json =>
      json.hcursor
        .downField("content")
        .downArray
        .downField("text")
        .as[String]
    }.getOrElse(throw new RuntimeException("Failed to parse response"))

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
