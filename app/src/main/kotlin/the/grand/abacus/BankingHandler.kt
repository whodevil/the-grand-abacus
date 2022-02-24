package the.grand.abacus

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.inject.Inject
import com.opencsv.CSVReader
import com.webcohesion.ofx4j.domain.data.ResponseEnvelope
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponseTransaction
import com.webcohesion.ofx4j.domain.data.common.TransactionList
import com.webcohesion.ofx4j.io.AggregateUnmarshaller
import mu.KotlinLogging
import the.grand.abacus.NamedGuiceObjectConstants.BANK_EXPORTS
import the.grand.abacus.NamedGuiceObjectConstants.PAYPAL_EXPORTS
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Named


private val logger = KotlinLogging.logger {}

class BankingHandler @Inject constructor(
    @Named(BANK_EXPORTS) private val bankExports: File,
    @Named(PAYPAL_EXPORTS) private val paypalExports: File,
    private val groupingRules: GroupingRules
) {
    fun readBankExport(): Map<String, List<Transaction>> {
        logger.info { "parsing bank exports" }
        return bankExports.listFiles()!!.map { file ->
            val transactionList = transactionList(file)
            val account = file.name.split(" ").first()
            file.name.dropLast(4) to transactionList.transactions.map { transaction ->
                val type = transactionType(transaction)
                val group = groupingRules.match(transaction.name, transaction.memo, TransactionSource.BANK)
                val date = ZonedDateTime.ofInstant(transaction.datePosted.toInstant(), ZoneId.systemDefault())
                Transaction(group, type, transaction.name, date, transaction.amount, TransactionSource.BANK, transaction.memo, account)
            }
        }.toList().fold(mutableMapOf<String, List<Transaction>>()) { acc, pair ->
            val key = pair.first.split(" ").last()
            acc[key] = acc[key]?.plus(pair.second) ?: pair.second
            acc
        }
    }

    private fun transactionList(file: File): TransactionList {
        val aggregateUnmarshaller = AggregateUnmarshaller(ResponseEnvelope::class.java)
        val unmarshal = aggregateUnmarshaller.unmarshal(file.reader(Charsets.UTF_8))
        return (unmarshal.messageSets.last().responseMessages.first() as BankStatementResponseTransaction).message.transactionList
    }

    private fun transactionType(transaction: com.webcohesion.ofx4j.domain.data.common.Transaction) =
        when {
            transaction.transactionType.equals(com.webcohesion.ofx4j.domain.data.common.TransactionType.DEBIT) -> TransactionType.DEBIT
            transaction.transactionType.equals(com.webcohesion.ofx4j.domain.data.common.TransactionType.CREDIT) -> TransactionType.CREDIT
            else -> throw java.lang.UnsupportedOperationException("Found Unknown Transaction Type")
        }

    fun readPaypalExport(): Map<String, List<Transaction>> {
        logger.info { "parsing paypal exports" }
        return paypalExports.listFiles()!!.associate { file ->
            val completed: List<Transaction>
            file.reader(Charsets.UTF_8).use { reader ->
                val csvReader = CSVReader(reader).readAll()
                csvReader.removeFirst()

                completed = csvReader.filter {
                    it[5].equals("Completed")
                }.filter {
                    // Lyft requires special handling because it never has a non-PreApproved state
                    !it[4].startsWith("PreApproved") || it[3].equals("Lyft")
                }.map {
                    buildTransactionFromPaypalData(it)
                }.filter {
                    it.amount != 0.0
                }
            }
            file.name.dropLast(4) to completed
        }
    }

    private fun buildTransactionFromPaypalData(row: Array<String>): Transaction {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z")
        val zonedDateTime = ZonedDateTime.parse("${row[0]} ${row[1]} ${row[2]}", formatter)

        val amount = if (row[7].isEmpty()) 0.0 else row[7].toDouble()
        val transactionType = when {
            amount <= 0 -> TransactionType.DEBIT
            amount > 0 -> TransactionType.CREDIT
            else -> throw java.lang.UnsupportedOperationException("Found an unknown transaction type")
        }
        val vendor = row[3]
        val memo = row[4]
        return Transaction(
            group = groupingRules.match(vendor, memo, TransactionSource.PAYPAL),
            type = transactionType,
            vendor = vendor,
            date = zonedDateTime,
            amount = amount,
            source = TransactionSource.PAYPAL,
            memo = row[4],
            ""
        )
    }
}

enum class Group(name: String) {
    PAYPAL("paypal"),
    AMAZON("amazon"),
    OTHER("other"),
    INCOME("income"),
    UTILITIES("utilities"),
    DINING_OUT("dining out"),
    GROCERIES("groceries"),
    TRANSPORTATION("transportation"),
    HOUSE("house"),
    SAVINGS("savings"),
    BARS("bars"),
    CABLE("cable"),
    STUDENT_DEBT("student debt"),
    RENT("rent"),
    HOBBIES("hobbies")
}

enum class TransactionType {
    CREDIT,
    DEBIT
}

enum class TransactionSource(name: String) {
    PAYPAL("paypal"),
    BANK("bank")
}

enum class TransactionField(name: String) {
    NAME("name"),
    MEMO("memo")
}

data class Transaction(
    val group: Group,
    val type: TransactionType,
    val vendor: String,
    val date: ZonedDateTime,
    val amount: Double,
    val source: TransactionSource,
    val memo: String,
    val account: String
)
