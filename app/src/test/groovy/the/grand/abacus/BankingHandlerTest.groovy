package the.grand.abacus

import org.assertj.core.util.Lists
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import static the.grand.abacus.Group.AMAZON
import static the.grand.abacus.Group.DINING_OUT
import static the.grand.abacus.Group.GROCERIES
import static the.grand.abacus.Group.INCOME
import static the.grand.abacus.Group.OTHER
import static the.grand.abacus.Group.TRANSPORTATION
import static the.grand.abacus.TestingConstants.*
import static the.grand.abacus.TestingConstants.CREDIT_NAME
import static the.grand.abacus.TestingConstants.CREDIT_NAME
import static the.grand.abacus.TransactionSource.BANK
import static the.grand.abacus.TransactionSource.PAYPAL
import static the.grand.abacus.TransactionType.CREDIT
import static the.grand.abacus.TransactionType.DEBIT

class BankingHandlerTest extends Specification {

    @Shared
    @TempDir
    File bankingExportsDir

    @Shared
    @TempDir
    File paypalExportsDir

    @Shared
    BankingHandler bankingHandler

    def setupSpec() {
        new File(bankingExportsDir, "${FILE_DATE}.ofx") << OFX_DATA
        new File(paypalExportsDir, "${FILE_DATE}.csv") << PAYPAL_CSV
        def groupingRules = new GroupingRules(
                Mock(SheetUtils),
                Lists.newArrayList(
                        new GroupingRule("Lyft", TRANSPORTATION, PAYPAL, DEBIT, TransactionField.NAME),
                        new GroupingRule("GrubHub", DINING_OUT, PAYPAL, DEBIT, TransactionField.NAME),
                        new GroupingRule(CREDIT_NAME, INCOME, BANK, CREDIT, TransactionField.NAME),
                        new GroupingRule("PAYPAL", Group.PAYPAL, BANK, DEBIT, TransactionField.NAME),
                        new GroupingRule("Instacart", GROCERIES, PAYPAL, DEBIT, TransactionField.NAME),
                        new GroupingRule("AMAZON", AMAZON, BANK, DEBIT, TransactionField.NAME)
                ))
        bankingHandler = new BankingHandler(bankingExportsDir, paypalExportsDir, groupingRules)
    }

    @Unroll
    def "test reading #matcher"() {
        when:
        List<Transaction> observed = dataSet[FILE_DATE].findAll {
            it.vendor.contains(matcher) && it.group == group && it.type == type && it.source == source
        }

        then:
        dataSet[FILE_DATE] != null
        observed.size() == expected

        where:
        dataSet                           | matcher     | group          | type   | source || expected
        bankingHandler.readBankExport()   | "PAYPAL"    | Group.PAYPAL   | DEBIT  | BANK   || 1
        bankingHandler.readBankExport()   | CREDIT_NAME | INCOME         | CREDIT | BANK   || 1
        bankingHandler.readBankExport()   | "AMAZON"    | AMAZON         | DEBIT  | BANK   || 1
        bankingHandler.readPaypalExport() | "Adobe"     | OTHER          | DEBIT  | PAYPAL || 1
        bankingHandler.readPaypalExport() | "GrubHub"   | DINING_OUT     | DEBIT  | PAYPAL || 1
        bankingHandler.readPaypalExport() | "Instacart" | GROCERIES      | DEBIT  | PAYPAL || 1
        bankingHandler.readPaypalExport() | "Lyft"      | TRANSPORTATION | DEBIT  | PAYPAL || 1
    }
}
