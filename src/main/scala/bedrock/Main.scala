package bedrock

import bedrock.Aws.bedrockClient
import bedrock.Config.config
import bedrock.Google.{docsService, extractDocumentId, extractTextFromDocument}
import io.circe.*
import io.circe.parser.*
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.io.Source
import scala.util.Using

@main def askBedrock(docUrl: String): Unit =
  try
    val docId = extractDocumentId(docUrl)

    println(s"Reading runbook from Google Doc: $docId")

    val document = docsService.documents().get(docId).execute()

    val runbookContent = extractTextFromDocument(document)

    println(s"Successfully read ${runbookContent.length} characters from Google Doc")
    println("=" * 60)

    val promptTemplate = Using.resource(Source.fromFile(config.app.promptFile))(_.mkString)

    val prompt = promptTemplate.replace("{RUNBOOK_CONTENT}", runbookContent)

    val requestBody = Json.obj(
      "messages" -> Json.arr(
        Json.obj(
          "role" -> Json.fromString("user"),
          "content" -> Json.fromString(prompt)
        )
      ),
      "max_tokens" -> Json.fromInt(config.aws.model.maxTokens),
      "anthropic_version" -> Json.fromString(config.aws.model.anthropicVersion)
    ).noSpaces

    println("Generating Mermaid diagram from runbook...")
    println("=" * 60)

    val request = InvokeModelRequest.builder()
      .modelId(config.aws.model.id)
      .body(SdkBytes.fromUtf8String(requestBody))
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
