package the.grand.abacus

import com.google.api.services.sheets.v4.Sheets
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.util.Modules
import com.google.inject.Provides
import com.google.inject.Singleton
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class GuiceInjectTest extends Specification {

    @Shared
    @TempDir
    File testConfigDir

    def setup() {
        def appProperties = new File(testConfigDir, GuiceConstants.APP_PROPERTIES)
        appProperties << """
        spreadsheetId=SOME_SPREADSHEET_ID
        userId=user
        appName=abacus
        """.stripIndent()

        def bankDir = new File(testConfigDir, GuiceConstants.BANK_REPORT_DIR)
        bankDir.mkdirs()

        def paypalDir = new File(testConfigDir, GuiceConstants.PAYPAL_REPORT_DIR)
        paypalDir.mkdirs()

        def credentialsJson = new File(testConfigDir, GuiceConstants.CREDENTIALS_JSON)
        credentialsJson << """
        {"installed":{
            "client_id":"CLIENT_ID",
            "project_id":"PROJECT_ID",
            "auth_uri":"https://accounts.google.com/o/oauth2/auth",
            "token_uri":"https://oauth2.googleapis.com/token",
            "auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs",
            "client_secret":"CLIENT_SECRET",
            "redirect_uris":["urn:ietf:wg:oauth:2.0:oob","http://localhost"]}}
        """.stripIndent()
    }

    def "test injection"() {
        given:
        def sheets = Mock(Sheets)
        def overrides =
                new AbstractModule() {
                    @Provides
                    @Singleton
                    Sheets sheets() {
                        return sheets
                    }
                }

        def injector = Guice.createInjector(Modules.override(new AbacusModule(testConfigDir)).with(overrides))

        when:
        def main = injector.getInstance(App.class)

        then:
        main != null
    }
}
