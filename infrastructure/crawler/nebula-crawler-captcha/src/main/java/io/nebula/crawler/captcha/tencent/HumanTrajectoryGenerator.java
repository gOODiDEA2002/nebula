package io.nebula.crawler.captcha.tencent;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 人类轨迹生成器
 * 
 * 基于物理加速度模型生成模拟人类滑动的轨迹
 * 包含加速-减速-过冲-回调等特征
 *
 * @author Nebula Team
 * @since 2.0.2
 */
@Slf4j
public class HumanTrajectoryGenerator {

    private final Random random = new Random();

    /**
     * 轨迹点
     * 
     * @param deltaX X轴增量
     * @param deltaY Y轴增量（抖动）
     * @param delay  延迟时间（毫秒）
     */
    public record TrajectoryPoint(double deltaX, double deltaY, long delay) {
    }

    /**
     * 生成人类滑动轨迹
     * 
     * 模拟人类滑动特征：
     * 1. 加速阶段（前 7/8 距离）：加速度 2-4
     * 2. 减速阶段（后 1/8 距离）：加速度 -3 到 -5
     * 3. 过冲：超过目标约 10 像素
     * 4. 回调：分两阶段回调到目标位置
     *
     * @param totalOffset 目标滑动距离（像素）
     * @return 轨迹点列表
     */
    public List<TrajectoryPoint> generate(int totalOffset) {
        List<TrajectoryPoint> points = new ArrayList<>();

        // 物理参数
        double v = 0;      // 初速度
        double t = 0.2;    // 单位时间 0.2s
        double current = 0; // 当前位移

        // 过冲距离：固定 10 像素
        int overshootDistance = 10;
        int extendedDistance = totalOffset + overshootDistance;

        // 减速阈值：distance * 7/8
        double mid = totalOffset * 7.0 / 8.0;

        log.debug("轨迹策略: 目标={}, 过冲={}, 减速点={}, 总滑动={}",
                totalOffset, overshootDistance, (int) mid, extendedDistance);

        // 阶段1：加速-减速运动
        while (current < extendedDistance) {
            int a;
            if (current < mid) {
                // 加速阶段：加速度随机 2-4
                a = 2 + random.nextInt(3);
            } else {
                // 减速阶段：加速度随机 -3 到 -5
                a = -(3 + random.nextInt(3));
            }

            double v0 = v;
            // 位移公式: s = v0*t + 0.5*a*t^2
            double s = v0 * t + 0.5 * a * (t * t);

            // 确保每次移动距离为正
            if (s <= 0 && current < extendedDistance) {
                s = 1;
            }

            current += s;

            // 速度更新: v = v0 + a*t
            v = v0 + a * t;

            // 速度下限保护
            if (v < 0) {
                v = 0;
            }

            // 确保不超过目标
            if (current > extendedDistance) {
                s = s - (current - extendedDistance);
                current = extendedDistance;
            }

            // 添加轨迹点：增量、Y轴随机抖动、延迟
            double deltaY = (random.nextDouble() - 0.5) * 2; // -1 到 1 的抖动
            long delay = 10 + random.nextInt(20); // 10-30ms 延迟
            points.add(new TrajectoryPoint(Math.round(s), deltaY, delay));
        }

        // 阶段2：反向回调
        // 第一阶段回调：4 步，每步 -2 到 -3
        for (int i = 0; i < 4; i++) {
            int back = -(2 + random.nextInt(2));
            double deltaY = (random.nextDouble() - 0.5) * 1;
            long delay = 20 + random.nextInt(30);
            points.add(new TrajectoryPoint(back, deltaY, delay));
        }

        // 第二阶段回调：4 步，每步 -1 到 -3
        for (int i = 0; i < 4; i++) {
            int back = -(1 + random.nextInt(3));
            double deltaY = (random.nextDouble() - 0.5) * 0.5;
            long delay = 30 + random.nextInt(40);
            points.add(new TrajectoryPoint(back, deltaY, delay));
        }

        log.debug("生成轨迹点数: {}", points.size());

        return points;
    }

    /**
     * 生成简单轨迹（备用方案）
     * 
     * 使用贝塞尔曲线风格的缓动函数
     *
     * @param totalOffset 目标滑动距离
     * @param steps       步数
     * @return 轨迹点列表
     */
    public List<TrajectoryPoint> generateSimple(int totalOffset, int steps) {
        List<TrajectoryPoint> points = new ArrayList<>();
        double lastX = 0;

        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / steps;
            // 使用缓动函数: ease-out-cubic
            double eased = 1 - Math.pow(1 - progress, 3);
            double currentX = totalOffset * eased;
            double deltaX = currentX - lastX;
            lastX = currentX;

            double deltaY = (random.nextDouble() - 0.5) * 4;
            long delay = 15 + random.nextInt(10);
            points.add(new TrajectoryPoint(deltaX, deltaY, delay));
        }

        return points;
    }

    /**
     * 格式化轨迹用于日志
     */
    public String formatTrajectory(List<TrajectoryPoint> trajectory) {
        StringBuilder sb = new StringBuilder("[");
        int count = 0;
        for (TrajectoryPoint p : trajectory) {
            if (count > 0) sb.append(", ");
            sb.append((int) p.deltaX());
            count++;
            if (count > 15) {
                sb.append(", ...(共").append(trajectory.size()).append("个)");
                break;
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
