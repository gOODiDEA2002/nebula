package io.nebula.task.scheduled;

/**
 * 每小时任务接口
 * 
 * <p>实现此接口的Bean将被自动调用，每小时执行一次。</p>
 * 
 * <p>使用方式：</p>
 * <pre>{@code
 * @Component
 * public class MyHourlyTask implements EveryHourExecute {
 *     
 *     @Override
 *     public void execute() {
 *         // 具体的任务逻辑
 *         log.info("每小时任务执行");
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>注意：</strong>需要在 XXL-JOB 管理后台手动配置任务：</p>
 * <ul>
 *   <li>JobHandler: {@code everyHourExecuteJobHandler}</li>
 *   <li>Cron: {@code 0 0 * * * ?} (每小时执行)</li>
 * </ul>
 * 
 * @author Nebula Framework
 */
public interface EveryHourExecute {

    /**
     * 执行每小时任务
     */
    void execute();
}
