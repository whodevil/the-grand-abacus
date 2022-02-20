package the.grand.abacus

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.inject.Inject

class SheetUtils @Inject constructor(
    private val configuration: Configuration,
    private val service: Sheets
) {
    fun post(
        body: ValueRange,
        range: String
    ) {
        service.spreadsheets().values().update(configuration.spreadSheetId(), range, body)
            .setValueInputOption("RAW")
            .execute()
    }

    fun createTab(title: String, setIndex: Boolean = false, index: Int = 0) {
        val sheets = getSheetsObject()
        val found = sheets.sheets.find {
            it.properties.title == title
        }
        if (found == null) {
            val sheetProperties = SheetProperties().setTitle(title)
            if (setIndex) {
                sheetProperties.index = index
            }
            val request = Request().setAddSheet(AddSheetRequest().setProperties(sheetProperties))
            postSheetCreate(request)
        }
    }

    private fun postSheetCreate(request: Request) {
        val requests: List<Request> = listOf(request)
        val body = BatchUpdateSpreadsheetRequest().setRequests(requests)
        service.spreadsheets().batchUpdate(configuration.spreadSheetId(), body).execute()
    }

    private fun getSheetsObject() = service.spreadsheets().get(configuration.spreadSheetId()).execute()

}
