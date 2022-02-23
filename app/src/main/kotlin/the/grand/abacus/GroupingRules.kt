package the.grand.abacus

import com.google.api.services.sheets.v4.model.ValueRange
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import com.google.inject.Inject
import com.google.inject.Singleton

@OpenForTesting
@Singleton
class GroupingRules @Inject constructor(private val sheetUtils: SheetUtils) {

    @VisibleForTesting
    constructor(sheetUtils: SheetUtils, rules: ArrayList<GroupingRule>) : this(sheetUtils) {
        this.rules = rules
    }

    companion object {
        const val GROUP_RULES_END_COLUMN = "E"
        const val GROUP_RULES_START_COLUMN = "A"
        const val GROUP_RULES = "Group Rules"
    }

    private var rules: ArrayList<GroupingRule> = Lists.newArrayList()

    @VisibleForTesting
    fun getRules(): ArrayList<GroupingRule> {
        return rules
    }

    fun refreshGroupRulesInfo() {
        sheetUtils.createTab(Companion.GROUP_RULES, true)

        val groupDataHeaders =
            Lists.newArrayList<List<Any>>(listOf("Matcher", "Group Name", "Source", "Transaction Type", "Field"))
        sheetUtils.post(ValueRange().setValues(groupDataHeaders), "${Companion.GROUP_RULES}!A1")

        val availableGroupNames = Lists.newArrayList<List<Any>>(listOf("Group Names"))
        Group.values().forEach { availableGroupNames.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableGroupNames), "${Companion.GROUP_RULES}!F1")

        val availableSourceNames = Lists.newArrayList<List<Any>>(listOf("Source Names"))
        TransactionSource.values().forEach { availableSourceNames.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableSourceNames), "${Companion.GROUP_RULES}!G1")

        val availableTransactionTypes = Lists.newArrayList<List<Any>>(listOf("Transaction Types"))
        TransactionType.values().forEach { availableTransactionTypes.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableTransactionTypes), "${Companion.GROUP_RULES}!H1")

        val availableFields = Lists.newArrayList<List<Any>>(listOf("Fields"))
        TransactionField.values().forEach { availableFields.add(listOf(it.name)) }
        sheetUtils.post(ValueRange().setValues(availableFields), "${Companion.GROUP_RULES}!I1")

        fetchRules()
    }

    fun fetchRules() {
        val returnValue =
            sheetUtils.fetchValues("${Companion.GROUP_RULES}!${Companion.GROUP_RULES_START_COLUMN}2:${Companion.GROUP_RULES_END_COLUMN}")
        returnValue.forEach { row ->
            rules.add(
                GroupingRule(
                    matcher = row[0] as String,
                    group = Group.valueOf(row[1] as String),
                    source = TransactionSource.valueOf(row[2] as String),
                    type = TransactionType.valueOf(row[3] as String),
                    field = TransactionField.valueOf(row[4] as String)
                )
            )
        }
    }

    fun match(name: String, memo: String, source: TransactionSource): Group {
        return rules.firstOrNull {
            name.contains(it.matcher) && source == it.source
        }?.group ?: Group.OTHER
    }
}

data class GroupingRule(
    val matcher: String,
    val group: Group,
    val source: TransactionSource,
    val type: TransactionType,
    val field: TransactionField
)
