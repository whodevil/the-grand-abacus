package the.grand.abacus

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.common.collect.Maps
import com.google.inject.Guice
import com.google.inject.Inject
import dev.misfitlabs.kotlinguice4.getInstance
import mu.KotlinLogging

import org.slf4j.bridge.SLF4JBridgeHandler

private val logger = KotlinLogging.logger {}

class App @Inject constructor(
    private val bankingHandler: BankingHandler,
    private val configuration: Configuration,
    transport: NetHttpTransport,
    jsonFactory: JsonFactory,
    credentialFactory: CredentialFactory
) {

    private val service = Sheets
        .Builder(transport, jsonFactory, credentialFactory.getCredentials())
        .setApplicationName(configuration.appName())
        .build()

    fun go() {
        val bankTransactions = bankingHandler.readBankExport().values.first().filter {
            it.group != Group.PAYPAL
        }
        val paypalData = bankingHandler.readPaypalExport().values.first()
        val transactions = bankTransactions + paypalData
        val bucket = Maps.newHashMap<Group, Double>()
        val rawValues = transactions
            .map {
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

        post(ValueRange().setValues(rawValues), "Sheet1!A1")
        post(bucketedValues, "Sheet1!E1")
    }

    private fun accumulateDebits(bucket: HashMap<Group, Double>, transaction: Transaction) {
        when (transaction.group) {
            Group.AMAZON -> accumulate(bucket, Group.AMAZON, transaction.amount)
            Group.PAYPAL -> accumulate(bucket, Group.PAYPAL, transaction.amount)
            else -> accumulate(bucket, Group.OTHER, transaction.amount)
        }
    }

    private fun post(
        body: ValueRange,
        range: String
    ) {
        service.spreadsheets().values().update(configuration.spreadSheetId(), range, body)
            .setValueInputOption("RAW")
            .execute()
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
