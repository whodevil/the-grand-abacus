package the.grand.abacus

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.inject.Inject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@OpenForTesting
class SheetUtils @Inject constructor(
    private val configuration: Configuration,
    private val service: Sheets
) {
    //TODO figure out the best way to make this more general and configurable
    fun post(
        body: ValueRange,
        sheetName: String,
        range: String
    ) {
        val found = findSheetByName(sheetName)
        batchUpdateSheetRequest(Request()
            .setRepeatCell(
                RepeatCellRequest()
                    .setCell(
                        CellData()
                            .setUserEnteredFormat(
                                CellFormat()
                                    .setBackgroundColor(
                                        Color()
                                            .setRed(java.lang.Float.valueOf("0"))
                                            .setGreen(java.lang.Float.valueOf("1"))
                                            .setBlue(java.lang.Float.valueOf("1"))
                                    )
                                    .setTextFormat(
                                        TextFormat()
                                            .setFontSize(12)
                                            .setBold(java.lang.Boolean.TRUE)
                                    )
                            )
                    )
                    .setRange(
                        GridRange()
                            .setSheetId(found!!.properties.sheetId)
                            .setStartRowIndex(0)
                            .setEndRowIndex(1)
                            .setStartColumnIndex(4)
                            .setEndColumnIndex(5)
                    )
                    .setFields("*")
            ))

        service.spreadsheets().values().update(configuration.spreadSheetId(), "${sheetName}!${range}", body)
            .setValueInputOption("RAW")
            .execute()
    }

    private fun findSheetByName(name: String): Sheet? {
        return getSheetsObject().sheets.find {
            it.properties.title == name
        }
    }

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
        val found = findSheetByName(title)
        if (found == null) {
            val sheetProperties = SheetProperties().setTitle(title)
            if (setIndex) {
                sheetProperties.index = index
            }
            val request = Request().setAddSheet(AddSheetRequest().setProperties(sheetProperties))
            batchUpdateSheetRequest(request)
        }
    }

    private fun batchUpdateSheetRequest(request: Request) {
        val requests: List<Request> = listOf(request)
        val body = BatchUpdateSpreadsheetRequest().setRequests(requests)
        service.spreadsheets().batchUpdate(configuration.spreadSheetId(), body).execute()
    }

    private fun getSheetsObject() = service.spreadsheets().get(configuration.spreadSheetId()).execute()

    fun fetchValues(range: String): MutableList<MutableList<Any>> {
        val response = service.spreadsheets().values()[configuration.spreadSheetId(), range]
            .execute()
        val values = response.getValues()
        return if (values == null || values.isEmpty()) {
            logger.info {
                "No values found at location $range"
            }
            mutableListOf(mutableListOf(mutableListOf("")))
        } else {
            values
        }
    }
}
