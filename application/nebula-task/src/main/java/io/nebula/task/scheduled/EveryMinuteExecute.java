package io.nebula.task.scheduled;

/**
 * 每分钟任务接口
 * 
 * <p>实现此接口的Bean将被自动调用，每分钟执行一次。</p>
 * 
 * <p>使用方式：</p>
 * <pre>{@code
 * @Component
 * public class MyMinuteTask implements EveryMinuteExecute {
 *     
 *     @Override
 *     public void execute() {
 *         // 具体的任务逻辑
 *         log.info("每分钟任务执行");
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>注意：</strong>需要在 XXL-JOB 管理后台手动配置任务：</p>
 * <ul>
 *   <li>JobHandler: {@code everyMinuteExecuteJobHandler}</li>
 *   <li>Cron: {@code 0 * * * * ?} (每分钟执行)</li>
 * </ul>
 * 
 * @author Nebula Framework
 */
public interface EveryMinuteExecute {

    /**
     * 执行每分钟任务
     */
    void execute();
}
