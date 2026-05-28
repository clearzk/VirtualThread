package zk.jdk.risk.result;

/**
 * 风控检查结果密封接口
 */
public sealed interface RiskCheckResult
        permits Passed, Rejected, ManualReview, Error {
    String requestId();
    String ruleId();
}
