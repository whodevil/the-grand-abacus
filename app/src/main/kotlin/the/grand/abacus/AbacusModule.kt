package the.grand.abacus

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.inject.Provides
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import java.io.File
import java.util.*
import javax.inject.Named

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
        return listOf(SheetsScopes.SPREADSHEETS)
    }

    @Provides
    @Singleton
    @Named("TOKENS_PATH")
    fun tokenPath(): File {
        return File("config/token")
    }

    @Provides
    @Singleton
    @Named("CREDENTIALS_JSON")
    fun credentialsJsonFile(): File {
        return File("config/credentials.json")
    }

    @Provides
    @Singleton
    @Named("CONFIG")
    fun configMap(): Properties {
        val config = Properties()
        config.load(File("config/app.properties").inputStream())
        return config
    }

    @Provides
    @Singleton
    @Named("BANK_EXPORTS")
    fun exports(): File {
        return File("config/bank")
    }

    @Provides
    @Singleton
    @Named("PAYPAL_EXPORTS")
    fun paypal(): File {
        return File("config/paypal")
    }
}
