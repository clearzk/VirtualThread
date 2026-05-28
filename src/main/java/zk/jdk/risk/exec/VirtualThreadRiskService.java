package zk.jdk.risk.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zk.jdk.risk.RiskCheckRule;
import zk.jdk.risk.result.*;
import zk.jdk.risk.result.Error;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于虚拟线程的高并发风控服务
 */
public class VirtualThreadRiskService {
    private static final Logger log = LoggerFactory.getLogger(VirtualThreadRiskService.class);
    private final ExecutorService virtualThreadExecutor;
    private final RuleExecutor ruleExecutor;
    private final List<RiskCheckRule> allRules;

    public VirtualThreadRiskService(List<RiskCheckRule> rules){
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.ruleExecutor = new RuleExecutor();
        this.allRules = List.copyOf(rules);
    }

    /**
     * 处理单个风控请求。为请求中的每条规则提交一个任务到虚拟线程执行器。
     * @param requestId 请求ID
     * @return 合并后的最终风控结果
     */
    public CompletableFuture<RiskCheckResult> processRequest(String requestId) {
        //为每条规则创建一个异步任务（由虚拟线程执行）
        List<CompletableFuture<RiskCheckResult>> futures = allRules.stream()
                .map(rule->CompletableFuture.supplyAsync(()->ruleExecutor.execute(rule, requestId) , virtualThreadExecutor))
                .toList();
        // 等待所有规则执行完成聚合结果
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v->futures.stream()
                        .map(CompletableFuture::join)
                        .reduce(this::aggregateResults)
                        .orElse(new Error(requestId, "AGGREGATION", new IllegalStateException("No results")))
                );
    }

    /**
     * 聚合多条规则的结果。使用模式匹配switch进行清晰的结果合并逻辑。
     * 这是一个简化的版本：任一Reject则整体Reject，有ManualReview则整体ManualReview，全Pass则Pass。
     */
    private RiskCheckResult aggregateResults(RiskCheckResult r1, RiskCheckResult r2) {
        // 再次使用模式匹配处理聚合逻辑，安全且清晰
        return switch (r1) {
            case Rejected rejected -> rejected; // 已有拒绝，直接返回
            case Error error -> error; // 已有错误，直接返回
            case ManualReview review -> {
                // 如果第一个是人工审核，检查第二个是否有更高优先级
                yield switch (r2) {
                    case Rejected rejected -> rejected; // Reject 优先级更高
                    case Error error -> error; // Error 优先级更高
                    default -> review; // 否则保持 ManualReview
                };
            }
            case Passed passed -> {
                // 如果第一个是通过，返回第二个结果（可能是 Pass、ManualReview、Rejected 或 Error）
                yield switch (r2) {
                    case Rejected rejected -> rejected; // 第二个是 Reject，整体 Reject
                    case ManualReview review -> review; // 第二个是 ManualReview，整体 ManualReview
                    case Error error -> error; // 第二个是 Error，整体 Error
                    case Passed ignored -> passed; // 两个都是 Pass，保持 Pass
                };
            }
        };
    }
}
