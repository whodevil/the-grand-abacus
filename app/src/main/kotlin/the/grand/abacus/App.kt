package the.grand.abacus

import com.google.api.services.sheets.v4.model.*
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.inject.Guice
import com.google.inject.Inject
import dev.misfitlabs.kotlinguice4.getInstance
import mu.KotlinLogging
import org.slf4j.bridge.SLF4JBridgeHandler
import java.time.format.DateTimeFormatter


private val logger = KotlinLogging.logger {}

class App @Inject constructor(
    private val bankingHandler: BankingHandler,
    private val sheetUtils: SheetUtils,
    private val groupingRules: GroupingRules,
    private val configuration: Configuration
) {
    private val columnLookup = mapOf(
        1 to "B",
        2 to "C",
        3 to "D",
        4 to "E",
        5 to "F",
        6 to "G",
        7 to "H",
        8 to "I",
        9 to "J",
        10 to "K",
        11 to "L",
        12 to "M",
        13 to "N"
    )

    fun go() {
        groupingRules.refreshGroupRulesInfo()
        val readBankExport = bankingHandler.readBankExport()
        readBankExport.keys.forEach { key ->
            val month = key.split(" ").last()
            sheetUtils.createTab(month)
            val year = updateYearData(month)
            val bankTransactions = readBankExport[month] ?: listOf()
            val paypalData = bankingHandler.readPaypalExport()[month]
            val transactions = bankTransactions + paypalData as List<Transaction>
            val bucket = Maps.newHashMap<Group, Double>()
            val rawValues = Lists.newArrayList<List<Any>>(listOf("Date", "Source", "Account", "Vendor", "Memo", "Type", "Group", "Amount"))
            rawValues.addAll(transactions.map {
                if(it.group == Group.OTHER) {
                    logger.info{
                        "OTHER TRANSACTION: ${it.amount} ${it.vendor}"
                    }
                }
                accumulate(bucket, it.group, it.amount)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                listOf(formatter.format(it.date), it.source.name, it.account, it.vendor, it.memo, it.type.toString(), it.group.toString(), it.amount)
            })
            val bucketedValues = ValueRange().setValues(Group.values().filter{
                !configuration.ignoreGroups().contains(it)
            }.map{
                listOf(bucket[it] ?: "")
            })
            sheetUtils.post(ValueRange().setValues(rawValues), "${month}!A1")
            val monthIndex = columnLookup[Integer.valueOf(month.split("-").last())]
            sheetUtils.post(bucketedValues, "${year}!${monthIndex}2")
            logger.info{"finished pushing data for $key, sleeping to not exceed googles rate limit"}
            Thread.sleep(60000)
        }
    }

    private fun updateYearData(month: String): String {
        val year = month.split("-").first()
        sheetUtils.createTab(year)
        val sheetId = sheetUtils.findSheetByName(year)!!.properties.sheetId
        val monthsGrid = GridRange().setSheetId(sheetId).setStartColumnIndex(1).setEndColumnIndex(14).setEndRowIndex(1).setStartRowIndex(0)
        sheetUtils.clearStyling(monthsGrid)
        sheetUtils.yellowCells(monthsGrid)
        val months = listOf(listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", "Total"))

        val keys = GridRange()
            .setSheetId(sheetId)
            .setStartColumnIndex(0)
            .setEndColumnIndex(1)
        sheetUtils.clearStyling(keys)
        sheetUtils.tealCells(keys
            .setStartRowIndex(1)
            .setEndRowIndex(Group.values().size + 1 - configuration.ignoreGroups().size))

        sheetUtils.post(ValueRange().setValues(months), "${year}!B1")
        val groups = ValueRange().setValues(Group.values().filter{
            !configuration.ignoreGroups().contains(it)
        }.map {
            listOf(it.name)
        } + listOf(listOf("")) + listOf(listOf("Total Bills")) + listOf(listOf("Net")))

        sheetUtils.post(groups, "${year}!A2")

        var index = 2
        while(index < Group.values().size - configuration.ignoreGroups().size + 2) {
            sheetUtils.postFormula(ValueRange().setValues(listOf(listOf("=SUM(B${index}:M${index})"))), "${year}!N${index}")
            index++
        }
        index = 1
        while(index < 14) {
            val column = columnLookup[index]
            val totalBills = Group.values().size - configuration.ignoreGroups().size - 1
            val totalBillsSumRow = Group.values().size - configuration.ignoreGroups().size + 3
            sheetUtils.postFormula(ValueRange().setValues(listOf(listOf("=SUM(${column}2:${column}${totalBills})"))), "${year}!${column}${totalBillsSumRow}")
            sheetUtils.postFormula(ValueRange().setValues(listOf(listOf("=SUM(${column}2:${column}${totalBills+1})"))), "${year}!${column}${totalBillsSumRow+1}")
            index++
        }
        return year
    }

    private fun accumulate(bucket: HashMap<Group, Double>, group: Group, amount: Double) {
        val value = bucket[group]
        if (value == null) {
            bucket[group] = amount
        } else {
            (value + amount).also { bucket[group] = it }
        }
    }
}

fun main() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    val injector = Guice.createInjector(AbacusModule())
    val main = injector.getInstance<App>()
    main.apply {
        go()
    }
}
