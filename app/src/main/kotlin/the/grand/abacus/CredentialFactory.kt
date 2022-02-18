package the.grand.abacus

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.inject.Inject
import java.io.File
import java.io.InputStreamReader
import javax.inject.Named

class CredentialFactory @Inject constructor(
    private val transport: NetHttpTransport,
    private val configuration: Configuration,
    private val jsonFactory: JsonFactory,
    @Named("SCOPES") private val scopes: List<String>,
    @Named("TOKENS_PATH") private val tokensPath: File,
    @Named("CREDENTIALS_JSON") private val credentialsFile: File
) {
    fun getCredentials(): Credential {
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(credentialsFile.inputStream()))
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
