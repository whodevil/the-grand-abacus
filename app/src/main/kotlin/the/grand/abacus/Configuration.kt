package the.grand.abacus

import com.google.inject.Inject
import java.util.*
import javax.inject.Named

class Configuration @Inject constructor(
    @Named("CONFIG") val config: Properties
) {

    fun appName(): String {
        return config.getProperty("appName")
    }

    fun spreadSheetId(): String {
        return config.getProperty("spreadsheetId")
    }

    fun userId(): String {
        return config.getProperty("userId")
    }
}
