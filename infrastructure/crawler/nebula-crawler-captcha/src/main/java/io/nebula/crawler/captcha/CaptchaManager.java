package io.nebula.crawler.captcha;

import io.nebula.crawler.captcha.detector.CaptchaDetector;
import io.nebula.crawler.captcha.exception.CaptchaException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 验证码管理器
 * 统一管理不同类型的验证码解决器
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class CaptchaManager {

    private final Map<CaptchaType, List<CaptchaSolver>> solvers;
    private final CaptchaDetector detector;

    /**
     * 构造函数
     *
     * @param solverList 解决器列表
     * @param detector   验证码检测器
     */
    public CaptchaManager(List<CaptchaSolver> solverList, CaptchaDetector detector) {
        this.detector = detector;
        this.solvers = solverList.stream()
                .collect(Collectors.groupingBy(
                        CaptchaSolver::getSupportedType,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    list.sort(Comparator.comparingInt(CaptchaSolver::getPriority));
                                    return list;
                                }
                        )
                ));

        log.info("CaptchaManager初始化完成，支持的验证码类型: {}",
                solvers.keySet().stream()
                        .map(CaptchaType::getDescription)
                        .collect(Collectors.joining(", ")));
    }

    /**
     * 解决验证码
     *
     * @param request 验证码请求
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();
        CaptchaType type = request.getType();

        // 如果未指定类型，尝试自动检测
        if (type == null || type == CaptchaType.UNKNOWN) {
            if (detector != null && request.getImageBase64() != null) {
                type = detector.detect(request.getImageBase64());
                request.setType(type);
                log.info("自动检测验证码类型: {}", type.getDescription());
            } else {
                throw new CaptchaException("未指定验证码类型且无法自动检测");
            }
        }

        List<CaptchaSolver> typeSolvers = solvers.get(type);
        if (typeSolvers == null || typeSolvers.isEmpty()) {
            throw new CaptchaException("不支持的验证码类型: " + type.getDescription());
        }

        // 按优先级尝试各个解决器
        CaptchaException lastException = null;
        for (CaptchaSolver solver : typeSolvers) {
            if (!solver.isAvailable()) {
                log.debug("解决器 {} 不可用，跳过", solver.getName());
                continue;
            }

            try {
                log.info("尝试使用 {} 识别验证码", solver.getName());
                CaptchaResult result = solver.solve(request);
                if (result.isSuccess()) {
                    result.setSolverName(solver.getName());
                    result.setCostTime(System.currentTimeMillis() - startTime);
                    result.setType(type);
                    log.info("验证码识别成功: solver={}, costTime={}ms",
                            solver.getName(), result.getCostTime());
                    return result;
                }
                log.warn("解决器 {} 识别失败: {}", solver.getName(), result.getErrorMessage());
            } catch (CaptchaException e) {
                log.warn("解决器 {} 抛出异常: {}", solver.getName(), e.getMessage());
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new CaptchaException("所有解决器均无法识别验证码");
    }

    /**
     * 异步解决验证码
     *
     * @param request 验证码请求
     * @return 识别结果的Future
     */
    public CompletableFuture<CaptchaResult> solveAsync(CaptchaRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return solve(request);
            } catch (CaptchaException e) {
                return CaptchaResult.fail(e.getMessage());
            }
        });
    }

    /**
     * 报告识别结果
     *
     * @param type    验证码类型
     * @param taskId  任务ID
     * @param success 是否成功
     */
    public void reportResult(CaptchaType type, String taskId, boolean success) {
        List<CaptchaSolver> typeSolvers = solvers.get(type);
        if (typeSolvers != null) {
            typeSolvers.forEach(s -> s.reportResult(taskId, success));
        }
    }

    /**
     * 检查指定类型的解决器是否可用
     *
     * @param type 验证码类型
     * @return true如果可用
     */
    public boolean isAvailable(CaptchaType type) {
        List<CaptchaSolver> typeSolvers = solvers.get(type);
        return typeSolvers != null && typeSolvers.stream().anyMatch(CaptchaSolver::isAvailable);
    }

    /**
     * 获取所有支持的验证码类型
     *
     * @return 支持的验证码类型集合
     */
    public Set<CaptchaType> getSupportedTypes() {
        return Collections.unmodifiableSet(solvers.keySet());
    }

    /**
     * 获取指定类型的可用解决器数量
     *
     * @param type 验证码类型
     * @return 可用解决器数量
     */
    public int getAvailableSolverCount(CaptchaType type) {
        List<CaptchaSolver> typeSolvers = solvers.get(type);
        if (typeSolvers == null) {
            return 0;
        }
        return (int) typeSolvers.stream().filter(CaptchaSolver::isAvailable).count();
    }
}
