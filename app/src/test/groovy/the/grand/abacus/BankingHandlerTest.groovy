package the.grand.abacus

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import static the.grand.abacus.Group.AMAZON
import static the.grand.abacus.Group.INCOME
import static the.grand.abacus.TestingConstants.*
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
        bankingHandler = new BankingHandler(bankingExportsDir, paypalExportsDir)
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
        dataSet                           | matcher     | group        | type   | source || expected
        bankingHandler.readBankExport()   | "PAYPAL"    | Group.PAYPAL | DEBIT  | BANK   || 1
        bankingHandler.readBankExport()   | CREDIT_NAME | INCOME       | CREDIT | BANK   || 1
        bankingHandler.readBankExport()   | "AMAZON"    | AMAZON       | DEBIT  | BANK   || 1
        bankingHandler.readPaypalExport() | "Adobe"     | Group.PAYPAL | DEBIT  | PAYPAL || 1
        bankingHandler.readPaypalExport() | "GrubHub"   | Group.PAYPAL | DEBIT  | PAYPAL || 1
        bankingHandler.readPaypalExport() | "Instacart" | Group.PAYPAL | DEBIT  | PAYPAL || 1
        bankingHandler.readPaypalExport() | "Lyft"      | Group.PAYPAL | DEBIT  | PAYPAL || 1
    }
}
