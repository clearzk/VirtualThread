package zk.jdk.risk.result;

public record Error(String requestId, String ruleId, Throwable cause) implements RiskCheckResult{
    Throwable exception() {return  cause.getCause();}
}
