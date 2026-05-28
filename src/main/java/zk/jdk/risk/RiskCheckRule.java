package zk.jdk.risk;

/**
 * 风控规则密封接口
 * 明确了系统中所有可能的风控规则类型，编译器知晓其完整集合
 */
public sealed interface RiskCheckRule
        permits AmountLimitRule, BlackListRule,BehaviorRule{
    String ruleId();
    String description();
}
