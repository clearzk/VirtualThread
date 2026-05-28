package zk.jdk.risk.exec;

import zk.jdk.risk.AmountLimitRule;
import zk.jdk.risk.BehaviorRule;
import zk.jdk.risk.BlackListRule;
import zk.jdk.risk.RiskCheckRule;
import zk.jdk.risk.result.*;
import zk.jdk.risk.result.Error;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * VirtualThreadRiskService 的测试类
 */
public class VirtualThreadRiskServiceTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 虚拟线程风控服务测试开始 ===\n");

        // 1. 准备测试数据 - 创建各种规则
        List<RiskCheckRule> rules = List.of(
                new AmountLimitRule("RULE_001", "金额限制规则", new BigDecimal("10000")),
                new BlackListRule("RULE_002", "黑名单检查"),
                new BehaviorRule("RULE_003", "行为分析规则", "pattern_v1")
        );

        // 2. 创建服务实例
        VirtualThreadRiskService service = new VirtualThreadRiskService(rules);

        // 3. 测试单个请求处理
        testSingleRequest(service);

        // 4. 测试多个并发请求
        testMultipleConcurrentRequests(service);

        System.out.println("\n=== 测试完成 ===");
    }

    /**
     * 测试单个请求处理
     */
    private static void testSingleRequest(VirtualThreadRiskService service) {
        System.out.println("--- 测试1: 单个请求处理 ---");

        String requestId = "REQ_" + System.currentTimeMillis();
        System.out.println("发送请求: " + requestId);

        CompletableFuture<RiskCheckResult> future = service.processRequest(requestId);

        // 等待结果（最多等待5秒）
        try {
            RiskCheckResult result = future.get(5, TimeUnit.SECONDS);
            printResult(result);
        } catch (Exception e) {
            System.err.println("请求处理异常: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 测试多个并发请求
     */
    private static void testMultipleConcurrentRequests(VirtualThreadRiskService service)
            throws InterruptedException {
        System.out.println("--- 测试2: 多个并发请求 ---");

        int requestCount = 10;
        CountDownLatch latch = new CountDownLatch(requestCount);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            String requestId = "REQ_BATCH_" + i;

            service.processRequest(requestId).thenAccept(result -> {
                System.out.println("请求 [" + (index + 1) + "] " + requestId + " 完成: "
                        + getResultType(result));
                latch.countDown();
            }).exceptionally(ex -> {
                System.err.println("请求 [" + (index + 1) + "] 异常: " + ex.getMessage());
                latch.countDown();
                return null;
            });
        }

        // 等待所有请求完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        if (completed) {
            System.out.println("\n所有 " + requestCount + " 个请求已完成");
            System.out.println("总耗时: " + (endTime - startTime) + " ms");
            System.out.println("平均每个请求耗时: " + ((endTime - startTime) / requestCount) + " ms");
        } else {
            System.err.println("部分请求超时！");
        }
    }

    /**
     * 打印详细结果
     */
    private static void printResult(RiskCheckResult result) {
        System.out.println("最终结果类型: " + getResultType(result));
        System.out.println("请求ID: " + result.requestId());
        System.out.println("规则ID: " + result.ruleId());

        // 根据结果类型打印额外信息
        switch (result) {
            case Passed passed ->
                    System.out.println("状态: ✓ 通过");
            case Rejected rejected ->
                    System.out.println("状态: ✗ 拒绝 - 原因: " + rejected.reason());
            case ManualReview review ->
                    System.out.println("状态: ⚠ 人工审核 - 备注: " + review.comment());
            case Error error ->
                    System.out.println("状态: ⚠ 错误 - " + error.cause().getMessage());
        }
    }

    /**
     * 获取结果类型的字符串表示
     */
    private static String getResultType(RiskCheckResult result) {
        return switch (result) {
            case Passed ignored -> "PASSED";
            case Rejected ignored -> "REJECTED";
            case ManualReview ignored -> "MANUAL_REVIEW";
            case Error ignored -> "ERROR";
        };
    }
}
