package bedrock

import bedrock.Config.config
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.UserCredentials

object Google:
  private val credentials = UserCredentials
    .newBuilder()
    .setClientId(config.google.clientId)
    .setClientSecret(config.google.clientSecret)
    .setRefreshToken(config.google.refreshToken)
    .build()

  private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
  private val jsonFactory = GsonFactory.getDefaultInstance

  val docsService: Docs = new Docs.Builder(
    httpTransport,
    jsonFactory,
    new HttpCredentialsAdapter(credentials)
  )
    .setApplicationName(config.google.applicationName)
    .build()

  // Google Docs URLs typically look like: https://docs.google.com/document/d/{DOCUMENT_ID}/edit
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
