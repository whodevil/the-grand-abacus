package the.grand.abacus

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.inject.Provides
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import the.grand.abacus.GuiceConstants.APP_PROPERTIES
import the.grand.abacus.GuiceConstants.BANK_REPORT_DIR
import the.grand.abacus.GuiceConstants.CONFIG_DIR
import the.grand.abacus.GuiceConstants.CREDENTIALS_JSON
import the.grand.abacus.GuiceConstants.PAYPAL_REPORT_DIR
import the.grand.abacus.GuiceConstants.TOKEN_DIR
import the.grand.abacus.NamedGuiceObjectConstants.BANK_EXPORTS
import the.grand.abacus.NamedGuiceObjectConstants.PAYPAL_EXPORTS
import the.grand.abacus.NamedGuiceObjectConstants.SCOPES
import the.grand.abacus.NamedGuiceObjectConstants.TOKENS_PATH
import java.io.File
import java.util.*
import javax.inject.Named

class AbacusModule : KotlinModule {

    private val configDir: File

    constructor() {
        configDir = File(CONFIG_DIR)
    }

    constructor(configDir: File) {
        this.configDir = configDir
    }

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
    @Named(SCOPES)
    fun scopes(): List<String> {
        return listOf(SheetsScopes.SPREADSHEETS)
    }

    @Provides
    @Singleton
    @Named(TOKENS_PATH)
    fun tokenPath(): File {
        return File(configDir, TOKEN_DIR)
    }

    @Provides
    @Singleton
    @Named(NamedGuiceObjectConstants.CREDENTIALS_JSON)
    fun credentialsJsonFile(): File {
        return File(configDir, CREDENTIALS_JSON)
    }

    @Provides
    @Singleton
    @Named(NamedGuiceObjectConstants.APP_PROPERTIES)
    fun configMap(): Properties {
        val config = Properties()
        config.load(File(configDir, APP_PROPERTIES).inputStream())
        return config
    }

    @Provides
    @Singleton
    @Named(BANK_EXPORTS)
    fun exports(): File {
        return File(configDir, BANK_REPORT_DIR)
    }

    @Provides
    @Singleton
    @Named(PAYPAL_EXPORTS)
    fun paypal(): File {
        return File(configDir, PAYPAL_REPORT_DIR)
    }

    @Provides
    @Singleton
    fun sheets(
        configuration: Configuration,
        credentialFactory: CredentialFactory,
        jsonFactory: JsonFactory,
        transport: NetHttpTransport
    ): Sheets {
        return Sheets
            .Builder(transport, jsonFactory, credentialFactory.getCredentials())
            .setApplicationName(configuration.appName())
            .build()
    }
}

object NamedGuiceObjectConstants {
    const val TOKENS_PATH = "TOKENS_PATH"
    const val BANK_EXPORTS = "BANK_EXPORTS"
    const val PAYPAL_EXPORTS = "PAYPAL_EXPORTS"
    const val APP_PROPERTIES = "APP_PROPERTIES"
    const val SCOPES = "SCOPES"
    const val CREDENTIALS_JSON = "CREDENTIALS_JSON"
}

object GuiceConstants {
    const val CONFIG_DIR = "config"
    const val TOKEN_DIR = "token"
    const val CREDENTIALS_JSON = "credentials.json"
    const val APP_PROPERTIES = "app.properties"
    const val BANK_REPORT_DIR = "bank"
    const val PAYPAL_REPORT_DIR = "paypal"
}
