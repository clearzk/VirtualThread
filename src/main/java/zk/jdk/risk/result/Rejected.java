package zk.jdk.risk.result;

public record Rejected(String requestId, String ruleId,String reason) implements RiskCheckResult {
}
