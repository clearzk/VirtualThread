package zk.jdk.risk.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zk.jdk.risk.AmountLimitRule;
import zk.jdk.risk.BehaviorRule;
import zk.jdk.risk.BlackListRule;
import zk.jdk.risk.RiskCheckRule;
import zk.jdk.risk.result.*;
import zk.jdk.risk.result.Error;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 规则执行器,模拟I/O阻塞操作
 */
public class RuleExecutor {
    private static final Logger log = LoggerFactory.getLogger(RuleExecutor.class);
    // 模拟一个会阻塞的规则执行方法
    public RiskCheckResult execute(RiskCheckRule rule, String requestId) {
        // 模拟I/O操作（网络调用、DB查询）的阻塞，虚拟线程在此处会被挂起
        try {
            // 随机睡眠50-200ms，模拟I/O延迟
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(50, 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Error(requestId, rule.ruleId(), e);
        }

        // 使用模式匹配switch，根据不同的规则类型执行不同的业务逻辑
        // 编译器会检查是否覆盖了 RiskCheckRule 的所有子类型
        return switch (rule) {
            case AmountLimitRule r -> {
                // 模拟基于金额的逻辑判断
                boolean pass = ThreadLocalRandom.current().nextBoolean();
                yield pass ?
                        new Passed(requestId, r.ruleId()) :
                        new Rejected(requestId, r.ruleId(), "Amount exceeds limit");
            }
            case BlackListRule r -> {
                // 模拟黑名单查询
                boolean inBlacklist = ThreadLocalRandom.current().nextDouble() < 0.1; // 10%概率命中
                yield inBlacklist ?
                        new Rejected(requestId, r.ruleId(), "User in blacklist") :
                        new Passed(requestId, r.ruleId());
            }
            case BehaviorRule r -> {
                // 模拟复杂行为分析，可能需要更长时间
                double score = ThreadLocalRandom.current().nextDouble();
                if (score > 0.9) {
                    yield new Rejected(requestId, r.ruleId(), "Suspicious behavior");
                } else if (score > 0.7) {
                    yield new ManualReview(requestId, r.ruleId(), "Needs further review");
                } else {
                    yield new Passed(requestId, r.ruleId());
                }
            }
            // 无需default分支，编译器已确保穷尽性
        };
    }
}
