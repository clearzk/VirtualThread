package zk.jdk.risk;

public record BehaviorRule(String ruleId,String description,String pattern)
        implements RiskCheckRule {
}
