package the.grand.abacus

import com.google.inject.Inject
import com.opencsv.CSVReader
import com.webcohesion.ofx4j.domain.data.ResponseEnvelope
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponseTransaction
import com.webcohesion.ofx4j.domain.data.common.TransactionList
import com.webcohesion.ofx4j.io.AggregateUnmarshaller
import mu.KotlinLogging
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Named


private val logger = KotlinLogging.logger {}

class BankingHandler @Inject constructor(
    @Named("BANK_EXPORTS") private val bankExports: File,
    @Named("PAYPAL_EXPORTS") private val paypalExports: File
) {
    fun readBankExport(): Map<String, List<Transaction>> {
        logger.info { "parsing bank exports" }
        return bankExports.listFiles()!!.associate { file->
            val transactionList = transactionList(file)
            file.name to transactionList.transactions.map { transaction ->
                val type = transactionType(transaction)
                val group = group(transaction)
                val date = ZonedDateTime.ofInstant(transaction.datePosted.toInstant(), ZoneId.systemDefault())
                Transaction(group,type, transaction.name, date, transaction.amount)
            }
        }
    }

    private fun transactionList(file: File): TransactionList {
        val aggregateUnmarshaller = AggregateUnmarshaller(ResponseEnvelope::class.java)
        val unmarshal = aggregateUnmarshaller.unmarshal(file.reader(Charsets.UTF_8))
        return (unmarshal.messageSets.last().responseMessages.first() as BankStatementResponseTransaction).message.transactionList
    }

    private fun transactionType(transaction: com.webcohesion.ofx4j.domain.data.common.Transaction) = when {
        transaction.transactionType.equals(com.webcohesion.ofx4j.domain.data.common.TransactionType.DEBIT) -> TransactionType.DEBIT
        transaction.transactionType.equals(com.webcohesion.ofx4j.domain.data.common.TransactionType.CREDIT) -> TransactionType.CREDIT
        else -> TransactionType.UNKNOWN
    }

    private fun group(transaction: com.webcohesion.ofx4j.domain.data.common.Transaction) = when {
        transaction.name.lowercase().contains("amazon") -> Group.AMAZON
        transaction.name.lowercase().contains("paypal") -> Group.PAYPAL
        else -> Group.OTHER
    }

    fun readPaypalExport(): Map<String, List<Transaction>> {
        logger.info { "parsing paypal exports" }

        return paypalExports.listFiles()!!.associate { file ->
            val completed: List<Transaction>
            file.reader(Charsets.UTF_8).use{ reader ->
                val csvReader = CSVReader(reader).readAll()
                csvReader.removeFirst()
                completed = csvReader.filter {
                    it[5].equals("Completed")
                }.filter {
                    !it[4].startsWith("PreApproved")
                }.map {
                    buildTransaction(it)
                }
            }
            file.name to completed
        }
    }

    private fun buildTransaction(row: Array<String>): Transaction {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z")
        val zonedDateTime = ZonedDateTime.parse("${row[0]} ${row[1]} ${row[2]}", formatter)

        val amount = if (row[7].isEmpty()) 0.0 else row[7].toDouble()
        val transactionType = when {
            amount <= 0 -> TransactionType.DEBIT
            amount > 0 -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }
        return Transaction(
            group = Group.PAYPAL,
            type = transactionType,
            vendor = row[3],
            date = zonedDateTime,
            amount = amount
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
    HOUSE_HOLD("house hold"),
    SAVINGS("savings")
}

enum class TransactionType {
    CREDIT,
    DEBIT,
    UNKNOWN
}

data class Transaction(val group: Group, val type: TransactionType, val vendor: String, val date: ZonedDateTime, val amount: Double)
