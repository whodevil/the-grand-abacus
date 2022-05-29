package the.grand.abacus

import com.google.common.collect.Lists
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

    def setupSpec() {
        new File(bankingExportsDir, "${FILE_DATE}.ofx") << OFX_DATA
        new File(paypalExportsDir, "${FILE_DATE}.csv") << PAYPAL_CSV
    }

    @Unroll
    def "test reading #matcher"() {
        given:
        def groupingRules = new GroupingRules(
                Mock(SheetUtils),
                Lists.newArrayList(
                        new GroupingRule("Lyft", TRANSPORTATION, PAYPAL, DEBIT, TransactionField.NAME, ""),
                        new GroupingRule("GrubHub", DINING_OUT, PAYPAL, DEBIT, TransactionField.NAME, ""),
                        new GroupingRule(CREDIT_NAME, INCOME, BANK, CREDIT, TransactionField.NAME, ""),
                        new GroupingRule("PAYPAL", Group.PAYPAL, BANK, DEBIT, TransactionField.NAME, ""),
                        new GroupingRule("Instacart", GROCERIES, PAYPAL, DEBIT, TransactionField.NAME, ""),
                        new GroupingRule("AMAZON", AMAZON, BANK, DEBIT, TransactionField.NAME, "")
                ))
        def bankingHandler = new BankingHandler(bankingExportsDir, paypalExportsDir, groupingRules)
        def dataSet = [:]
        dataSet[PAYPAL] = bankingHandler.readPaypalExport()
        dataSet[BANK] = bankingHandler.readBankExport()

        when:
        List<Transaction> observed = dataSet[source][FILE_DATE].findAll {
            it.vendor.contains(matcher) && it.group == group && it.type == type && it.source == source
        }

        then:
        dataSet[source][FILE_DATE] != null
        observed.size() == expected

        where:
        matcher     | group          | type   | source || expected
        "PAYPAL"    | Group.PAYPAL   | DEBIT  | BANK   || 1
        CREDIT_NAME | INCOME         | CREDIT | BANK   || 1
        "AMAZON"    | AMAZON         | DEBIT  | BANK   || 1
        "Adobe"     | OTHER          | DEBIT  | PAYPAL || 1
        "GrubHub"   | DINING_OUT     | DEBIT  | PAYPAL || 1
        "Instacart" | GROCERIES      | DEBIT  | PAYPAL || 1
        "Lyft"      | TRANSPORTATION | DEBIT  | PAYPAL || 1
    }

    @Unroll
    def "test default rules #matcher"() {
        given:
        def sheetUtils = Mock(SheetUtils)
        sheetUtils.fetchValues(GroupingRules.GROUP_RULES_QUERY) >> [[""]]
        def groupingRules = new GroupingRules(sheetUtils)
        def bankingHandler = new BankingHandler(bankingExportsDir, paypalExportsDir, groupingRules)
        groupingRules.fetchRules()
        def dataSet = bankingHandler.readBankExport()


        when:
        List<Transaction> observed = dataSet[FILE_DATE].findAll {
            it.vendor.contains(matcher) && it.group == group && it.type == type && it.source == source
        }

        then:
        dataSet[FILE_DATE] != null
        observed.size() == expected

        where:
        matcher  | group        | type  | source || expected
        "PAYPAL" | Group.PAYPAL | DEBIT | BANK   || 1
        "AMAZON" | AMAZON       | DEBIT | BANK   || 1
    }
}
