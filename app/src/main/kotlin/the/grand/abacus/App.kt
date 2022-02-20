package the.grand.abacus

import com.google.api.services.sheets.v4.model.*
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.inject.Guice
import com.google.inject.Inject
import dev.misfitlabs.kotlinguice4.getInstance
import mu.KotlinLogging
import org.slf4j.bridge.SLF4JBridgeHandler


private val logger = KotlinLogging.logger {}

private const val GROUP_RULES = "Group Rules"

class App @Inject constructor(
    private val bankingHandler: BankingHandler,
    private val sheetUtils: SheetUtils
) {
    fun go() {
        refreshGroupRulesInfo()
        val readBankExport = bankingHandler.readBankExport()
        readBankExport.keys.forEach { month ->
            sheetUtils.createTab(month)
            val bankTransactions = readBankExport[month]?.filter {
                it.group != Group.PAYPAL
            } as ArrayList<Transaction>
            val paypalData = bankingHandler.readPaypalExport()[month]
            val transactions = bankTransactions + paypalData as List<Transaction>
            val bucket = Maps.newHashMap<Group, Double>()
            val rawValues = transactions.map {
                when (it.type) {
                    TransactionType.DEBIT -> accumulateDebits(bucket, it)
                    TransactionType.CREDIT -> accumulate(bucket, Group.INCOME, it.amount)
                    else -> logger.info { "found unknown transaction type" }
                }
                listOf(it.vendor, it.type.toString(), it.amount)
            }
            val bucketedValues = ValueRange().setValues(bucket.keys.map {
                listOf(it.name, bucket[it])
            })
            sheetUtils.post(ValueRange().setValues(rawValues), "${month}!A1")
            sheetUtils.post(bucketedValues, "${month}!E1")
        }
    }

    private fun refreshGroupRulesInfo() {
        sheetUtils.createTab(GROUP_RULES, true)
        val categoryData = Lists.newArrayList<List<Any>>(listOf("Available Groups"))
        Group.values().forEach { categoryData.add(listOf(it.name)) }

        val groupDataHeaders = Lists.newArrayList<List<Any>>(listOf("Group Name", "Matcher"))
        sheetUtils.post(ValueRange().setValues(groupDataHeaders), "${GROUP_RULES}!A1")
        sheetUtils.post(ValueRange().setValues(categoryData), "${GROUP_RULES}!D1")
    }

    private fun accumulateDebits(bucket: HashMap<Group, Double>, transaction: Transaction) {
        when (transaction.group) {
            Group.AMAZON -> accumulate(bucket, Group.AMAZON, transaction.amount)
            Group.PAYPAL -> accumulate(bucket, Group.PAYPAL, transaction.amount)
            else -> accumulate(bucket, Group.OTHER, transaction.amount)
        }
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
