package the.grand.abacus

import com.google.inject.Inject
import the.grand.abacus.NamedGuiceObjectConstants.APP_PROPERTIES
import java.util.*
import javax.inject.Named

class Configuration @Inject constructor(
    @Named(APP_PROPERTIES) val config: Properties
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

    fun ignoreGroups(): List<Group> {
        return config.getProperty("ignoredGroups").split(",").map{
            Group.valueOf(it)
        }
    }
}
