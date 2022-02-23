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

    val bold = TextFormat()
        .setBold(java.lang.Boolean.TRUE)!!
    val teal = Color()
        .setRed(0f)
        .setGreen(1f)
        .setBlue(1f)!!

    val yellow = Color()
        .setRed(1f)
        .setGreen(1f)
        .setBlue(0f)!!

    fun yellowCells(gridRange: GridRange) {
        styling(gridRange,
            CellFormat()
                .setBackgroundColor(
                    yellow
                )
                .setTextFormat(
                    bold
                ))
    }

    fun tealCells(gridRange: GridRange) {
        styling(gridRange,
            CellFormat()
            .setBackgroundColor(
                teal
            )
            .setTextFormat(
                bold
            ))
    }

    fun styling(
        gridRange: GridRange,
        cellFormat: CellFormat
    ) {
        batchUpdateSheetRequest(
            Request()
                .setRepeatCell(
                    RepeatCellRequest()
                        .setCell(
                            CellData()
                                .setUserEnteredFormat(
                                    cellFormat
                                )
                        )
                        .setRange(gridRange)
                        .setFields("*")
                )
        )
    }

    fun clearStyling(
        gridRange: GridRange
    ) {
        batchUpdateSheetRequest(
            Request()
                .setRepeatCell(
                    RepeatCellRequest()
                        .setCell(
                            CellData()
                                .setUserEnteredFormat(
                                    CellFormat()
                                )
                        )
                        .setRange(gridRange)
                        .setFields("*")
                )
        )
    }

    fun findSheetByName(name: String): Sheet? {
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
