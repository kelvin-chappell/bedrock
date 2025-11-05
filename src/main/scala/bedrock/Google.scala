package bedrock

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.UserCredentials
import io.github.cdimascio.dotenv.Dotenv

object Google:
  private val dotenv = Dotenv.configure().load()

  private val clientId = Option(dotenv.get("GOOGLE_CLIENT_ID"))
    .getOrElse(throw new RuntimeException("GOOGLE_CLIENT_ID not set in .env file or environment"))

  private val clientSecret = Option(dotenv.get("GOOGLE_CLIENT_SECRET"))
    .getOrElse(throw new RuntimeException("GOOGLE_CLIENT_SECRET not set in .env file or environment"))

  private val refreshToken = Option(dotenv.get("GOOGLE_REFRESH_TOKEN"))
    .getOrElse(throw new RuntimeException("GOOGLE_REFRESH_TOKEN not set in .env file or environment"))

  private val credentials = UserCredentials
    .newBuilder()
    .setClientId(clientId)
    .setClientSecret(clientSecret)
    .setRefreshToken(refreshToken)
    .build()

  private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
  private val jsonFactory = GsonFactory.getDefaultInstance

  val docsService: Docs = new Docs.Builder(
    httpTransport,
    jsonFactory,
    new HttpCredentialsAdapter(credentials)
  )
    .setApplicationName("Bedrock Runbook Reader")
    .build()
