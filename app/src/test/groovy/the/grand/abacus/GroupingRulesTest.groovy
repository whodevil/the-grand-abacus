package the.grand.abacus

import com.google.common.collect.Lists
import spock.lang.Specification

import static the.grand.abacus.GroupingRules.GROUP_RULES
import static the.grand.abacus.GroupingRules.GROUP_RULES_END_COLUMN
import static the.grand.abacus.GroupingRules.GROUP_RULES_START_COLUMN

class GroupingRulesTest extends Specification {
    def "test fetching rules"() {
        given:
        String matcher = "PAYPAL"
        String group = "PAYPAL"
        String source = "BANK"
        String type = "CREDIT"
        String field = "NAME"
        def sheetUtils = Mock(SheetUtils)
        GroupingRules groupingRules = new GroupingRules(sheetUtils)

        when:
        groupingRules.fetchRules()
        Group foundGroup = groupingRules.match(matcher, "memo", TransactionSource.BANK, "")

        then:
        1 * sheetUtils.fetchValues("${GROUP_RULES}!${GROUP_RULES_START_COLUMN}2:${GROUP_RULES_END_COLUMN}") >> {
            def list = Lists.<ArrayList>newArrayList()
            list.add(Lists.newArrayList(matcher, group, source, type, field))
            return list
        }

        groupingRules.getRules().size() == 1
        foundGroup == Group.PAYPAL
    }
}
