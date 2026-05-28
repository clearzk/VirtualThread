package zk.jdk.risk.result;

public record Passed(String requestId,String ruleId) implements RiskCheckResult {
}
