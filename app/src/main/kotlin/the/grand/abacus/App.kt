package the.grand.abacus

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import mu.KotlinLogging
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Named


private val logger = KotlinLogging.logger {}

class App @Inject constructor(
    private val transport: NetHttpTransport,
    private val configuration: Configuration,
    private val jsonFactory: JsonFactory, private val credentialFactory: CredentialFactory
) {

    fun go() {
        val range = "Class Data!A2:E"
        val service = Sheets
            .Builder(transport, jsonFactory, credentialFactory.getCredentials())
            .setApplicationName(configuration.appName())
            .build()
        val response: ValueRange = service.spreadsheets().values()[configuration.spreadSheetId(), range]
            .execute()
        val values = response.getValues()
        if (values == null || values.isEmpty()) {
            logger.info("No data found.")
        } else {
            logger.info("Name, Major")
            for (row in values) {
                // Print columns A and E, which correspond to indices 0 and 4.
                logger.info("${row[0]}, ${row[4]}")
            }
        }
    }
}

class CredentialFactory @Inject constructor(
    private val transport: NetHttpTransport,
    private val configuration: Configuration,
    private val jsonFactory: JsonFactory,
    @Named("SCOPES") private val scopes: List<String>,
    @Named("TOKENS_PATH") private val tokensPath: File
) {
    fun getCredentials(): Credential {
        val `in`: InputStream =
            CredentialFactory::class.java.getResourceAsStream(configuration.credentialsPath())
                ?: throw FileNotFoundException("Resource not found: ${configuration.credentialsPath()}")
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(`in`))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            transport, jsonFactory, clientSecrets, scopes
        )
            .setDataStoreFactory(FileDataStoreFactory(tokensPath))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize(configuration.userId())
    }
}

class Configuration {
    fun appName(): String {
        TODO("Not yet implemented")
    }

    fun spreadSheetId(): String {
        TODO("Not yet implemented")
    }

    fun credentialsPath(): String {
        TODO("Not yet implemented")
    }

    fun tokenPath(): String {
        TODO("Not yet implemented")
    }

    fun userId(): String {
        TODO("Not yet implemented")
    }
}

class AbacusModule : KotlinModule() {
    @Provides
    @Singleton
    fun netHttpTransport(): NetHttpTransport {
        return GoogleNetHttpTransport.newTrustedTransport()
    }

    @Provides
    @Singleton
    fun jsonFactory(): JsonFactory {
        return GsonFactory.getDefaultInstance()
    }

    @Provides
    @Singleton
    @Named("SCOPES")
    fun scopes(): List<String> {
        return listOf(SheetsScopes.SPREADSHEETS_READONLY)
    }

    @Provides
    @Singleton
    @Named("TOKENS_PATH")
    fun tokenPath(configuration: Configuration): File {
        return File(configuration.tokenPath())
    }
}

fun main() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    val injector = Guice.createInjector(AbacusModule())
    val main = injector.getInstance<App>()
    main.apply {
        go()
    }
}
