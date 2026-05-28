package zk.jdk.risk.result;

public record ManualReview(String requestId, String ruleId,String comment) implements RiskCheckResult {
}
