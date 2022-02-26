package the.grand.abacus

import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import com.google.inject.Inject
import com.google.inject.Singleton
import the.grand.abacus.Group.AMAZON
import the.grand.abacus.Group.PAYPAL
import the.grand.abacus.TransactionField.MEMO
import the.grand.abacus.TransactionField.NAME
import the.grand.abacus.TransactionSource.BANK
import the.grand.abacus.TransactionType.*

@OpenForTesting
@Singleton
class GroupingRules @Inject constructor(private val sheetUtils: SheetUtils) {

    @VisibleForTesting
    constructor(sheetUtils: SheetUtils, rules: ArrayList<GroupingRule>) : this(sheetUtils) {
        this.rules = rules
    }

    companion object {
        const val GROUP_RULES_END_COLUMN = "G"
        const val GROUP_RULES_START_COLUMN = "B"
        const val GROUP_RULES = "Group Rules"
        const val GROUP_NAMES_KEY_ROW_NUMBER = 2
        const val GROUP_RULES_QUERY = "${GROUP_RULES}!${GROUP_RULES_START_COLUMN}2:${GROUP_RULES_END_COLUMN}"
    }

    private var rules: ArrayList<GroupingRule> = Lists.newArrayList()
    @VisibleForTesting
    val defaultRules = listOf(
        GroupingRule("AMAZON", AMAZON, BANK, DEBIT, NAME),
        GroupingRule("PAYPAL", PAYPAL, BANK, DEBIT, NAME)
    )

    @VisibleForTesting
    fun getRules(): ArrayList<GroupingRule> {
        return rules
    }

    fun refreshGroupRulesInfo() {
        sheetUtils.createTab(GROUP_RULES, true)
        val sheet = sheetUtils.findSheetByName(GROUP_RULES)
        val sheetId = sheet!!.properties.sheetId
        sheetUtils.clearStyling(GridRange().setSheetId(sheetId).setStartRowIndex(0).setEndRowIndex(1))
        sheetUtils.clearStyling(GridRange().setSheetId(sheetId).setStartColumnIndex(0).setEndColumnIndex(1))
        rulesHeader(sheetId)
        groupKeyInfo()
        val sourceNameRowNumber = sourceKeyInfo(sheetId)
        val transactionTypeRowNumber = transactionKeyInfo(sourceNameRowNumber, sheetId)
        fieldsKeyInfo(transactionTypeRowNumber, sheetId)
        fetchRules()
    }

    private fun rulesHeader(sheetId: Int?) {
        sheetUtils.yellowCells(
            GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(0)
                .setEndRowIndex(1)
                .setStartColumnIndex(1)
                .setEndColumnIndex(7)
        )

        sheetUtils.tealCells(
            GridRange()
                .setSheetId(sheetId)
                .setStartColumnIndex(0)
                .setEndColumnIndex(1)
                .setStartRowIndex(GROUP_NAMES_KEY_ROW_NUMBER - 1)
                .setEndRowIndex(GROUP_NAMES_KEY_ROW_NUMBER)
        )

        val groupDataHeaders =
            Lists.newArrayList<List<Any>>(listOf("Matcher", "Group Name", "Source", "Transaction Type", "Field", "Account"))
        sheetUtils.post(ValueRange().setValues(groupDataHeaders), "${GROUP_RULES}!B1")
    }

    private fun groupKeyInfo() {
        val availableGroupNames = Lists.newArrayList<List<Any>>(listOf("Group Names"))
        Group.values().forEach { availableGroupNames.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableGroupNames), "${GROUP_RULES}!A${GROUP_NAMES_KEY_ROW_NUMBER}")
    }

    private fun sourceKeyInfo(sheetId: Int?): Int {
        val sourceNameRowNumber = Group.values().size + 4
        sheetUtils.tealCells(
            GridRange()
                .setSheetId(sheetId)
                .setStartColumnIndex(0)
                .setEndColumnIndex(1)
                .setStartRowIndex(sourceNameRowNumber - 1)
                .setEndRowIndex(sourceNameRowNumber)
        )
        val availableSourceNames = Lists.newArrayList<List<Any>>(listOf("Source Names"))
        TransactionSource.values().forEach { availableSourceNames.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableSourceNames), "${GROUP_RULES}!A${sourceNameRowNumber}")
        return sourceNameRowNumber
    }

    private fun transactionKeyInfo(sourceNameRowNumber: Int, sheetId: Int?): Int {
        val transactionTypeRowNumber = sourceNameRowNumber + TransactionSource.values().size + 2
        sheetUtils.tealCells(
            GridRange()
                .setSheetId(sheetId)
                .setStartColumnIndex(0)
                .setEndColumnIndex(1)
                .setStartRowIndex(transactionTypeRowNumber - 1)
                .setEndRowIndex(transactionTypeRowNumber)
        )
        val availableTransactionTypes = Lists.newArrayList<List<Any>>(listOf("Transaction Types"))
        values().forEach { availableTransactionTypes.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableTransactionTypes), "$GROUP_RULES!A${transactionTypeRowNumber}")
        return transactionTypeRowNumber
    }

    private fun fieldsKeyInfo(transactionTypeRowNumber: Int, sheetId: Int?) {
        val fieldRowNumber = transactionTypeRowNumber + values().size + 2
        sheetUtils.tealCells(
            GridRange()
                .setSheetId(sheetId)
                .setStartColumnIndex(0)
                .setEndColumnIndex(1)
                .setStartRowIndex(fieldRowNumber - 1)
                .setEndRowIndex(fieldRowNumber)
        )
        val availableFields = Lists.newArrayList<List<Any>>(listOf("Fields"))
        TransactionField.values().forEach { availableFields.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableFields), "${GROUP_RULES}!A${fieldRowNumber}")
    }

    fun fetchRules() {
        val returnValue =
            sheetUtils.fetchValues(GROUP_RULES_QUERY)
        if(returnValue.isNotEmpty() && returnValue.first().size == 1) {
            rules += defaultRules
            return
        }
        returnValue.forEach { row ->
            val account = if(row.size == 6) row[5] as String else ""
            rules.add(
                GroupingRule(
                    matcher = row[0] as String,
                    group = Group.valueOf(row[1] as String),
                    source = TransactionSource.valueOf(row[2] as String),
                    type = valueOf(row[3] as String),
                    field = TransactionField.valueOf(row[4] as String),
                    account = account
                )
            )
        }
    }

    fun match(name: String, memo: String, source: TransactionSource, account: String = ""): Group {
        return rules.firstOrNull {
            val accountMatch = account.isEmpty() || it.account.isEmpty() || account.equals(it.account)
            val sourceMatch = source == it.source
            val fieldMatch = when(it.field) {
                NAME -> name.contains(it.matcher)
                MEMO -> memo.contains(it.matcher)
            }
            sourceMatch && accountMatch && fieldMatch
        }?.group ?: Group.OTHER
    }
}

data class GroupingRule(
    val matcher: String,
    val group: Group,
    val source: TransactionSource,
    val type: TransactionType,
    val field: TransactionField,
    val account: String = ""
)
