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
    private val groupingRules: GroupingRules
) {
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
            val rawValues = Lists.newArrayList<List<Any>>(listOf("Date", "Source", "Account", "Vendor", "Memo", "Type", "Amount"))
            rawValues.addAll(transactions.map {
                if(it.group == Group.OTHER) {
                    logger.info{
                        "OTHER TRANSACTION: ${it.vendor}"
                    }
                }
                accumulate(bucket, it.group, it.amount)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                listOf(formatter.format(it.date), it.source.name, it.account, it.vendor, it.memo, it.type.toString(), it.amount)
            })
            val bucketedValues = ValueRange().setValues(Group.values().map{
                listOf(it.name, bucket[it] ?: "")
            })
            sheetUtils.post(ValueRange().setValues(rawValues), "${month}!A1")
            sheetUtils.post(bucketedValues, "${year}!A2")
        }
    }

    private fun updateYearData(month: String): String {
        val year = month.split("-").first()
        sheetUtils.createTab(year)
        val months = listOf(listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"))
        sheetUtils.post(ValueRange().setValues(months), "${year}!B1")
        val groups = ValueRange().setValues(Group.values().map {
            listOf(it.name)
        })
        sheetUtils.post(groups, "${year}!A2")
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
