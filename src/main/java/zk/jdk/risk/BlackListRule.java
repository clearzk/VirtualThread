package zk.jdk.risk;

public record BlackListRule(String ruleId,String description)
        implements RiskCheckRule{
}
