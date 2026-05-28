package zk.jdk.risk;

import java.math.BigDecimal;

public record AmountLimitRule(String ruleId, String description, BigDecimal maxAmount)
        implements RiskCheckRule{
}
