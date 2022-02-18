package the.grand.abacus

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.inject.Guice
import com.google.inject.Inject
import dev.misfitlabs.kotlinguice4.getInstance
import mu.KotlinLogging
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.File


private val logger = KotlinLogging.logger {}

class App @Inject constructor(
    private val transport: NetHttpTransport,
    private val configuration: Configuration,
    private val jsonFactory: JsonFactory, private val credentialFactory: CredentialFactory
) {

    fun go() {
        val range = "Sheet1!A2:E2"
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

fun main() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    logger.info(File("./").absolutePath)
    val injector = Guice.createInjector(AbacusModule())
    val main = injector.getInstance<App>()
    main.apply {
        go()
    }
}
