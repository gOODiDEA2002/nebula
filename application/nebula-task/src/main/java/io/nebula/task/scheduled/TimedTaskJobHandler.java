package io.nebula.task.scheduled;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Timed Task Job Handler
 * 
 * This class serves as the unified task handler for XXL-JOB.
 *
 * @author Nebula Framework
 */
@Slf4j
@Component
public class TimedTaskJobHandler {

    @Autowired(required = false)
    private List<EveryMinuteExecute> everyMinuteExecutes;

    @Autowired(required = false)
    private List<EveryFiveMinuteExecute> everyFiveMinuteExecutes;

    @Autowired(required = false)
    private List<EveryHourExecute> everyHourExecutes;

    @Autowired(required = false)
    private List<EveryDayExecute> everyDayExecutes;

    /**
     * Every minute task scheduler
     */
    @XxlJob("everyMinuteExecuteJobHandler")
    public ReturnT<String> everyMinuteExecuteJobHandler(String param) {
        log.info("Every minute task started");
        if (everyMinuteExecutes == null || everyMinuteExecutes.isEmpty()) {
            log.info("No every minute tasks registered");
            return new ReturnT<>(200, "SUCCESS");
        }
        for (EveryMinuteExecute task : everyMinuteExecutes) {
            try {
                task.execute();
            } catch (Exception e) {
                log.error("Error executing every minute task: {}", task.getClass().getSimpleName(), e);
            }
        }
        log.info("Every minute task completed");
        return new ReturnT<>(200, "SUCCESS");
    }

    /**
     * Every 5 minutes task scheduler
     */
    @XxlJob("everyFiveMinuteExecuteJobHandler")
    public ReturnT<String> everyFiveMinuteExecuteJobHandler(String param) {
        log.info("Every 5 minutes task started");

        if (everyFiveMinuteExecutes == null || everyFiveMinuteExecutes.isEmpty()) {
            log.info("No every 5 minutes tasks registered");
            return new ReturnT<>(200, "SUCCESS");
        }

        for (EveryFiveMinuteExecute task : everyFiveMinuteExecutes) {
            try {
                task.execute();
            } catch (Exception e) {
                log.error("Error executing every 5 minutes task: {}", task.getClass().getSimpleName(), e);
            }
        }
        log.info("Every 5 minutes task completed");
        return new ReturnT<>(200, "SUCCESS");
    }

    /**
     * Every hour task scheduler
     */
    @XxlJob("everyHourExecuteJobHandler")
    public ReturnT<String> everyHourExecuteJobHandler(String param) {
        log.info("Every hour task started");
        if (everyHourExecutes == null || everyHourExecutes.isEmpty()) {
            log.info("No every hour tasks registered");
            return new ReturnT<>(200, "SUCCESS");
        }
        for (EveryHourExecute task : everyHourExecutes) {
            try {
                task.execute();
            } catch (Exception e) {
                log.error("Error executing every hour task: {}", task.getClass().getSimpleName(), e);
            }
        }
        log.info("Every hour task completed");
        return new ReturnT<>(200, "SUCCESS");
    }

    /**
     * Every day task scheduler
     */
    @XxlJob("everyDayExecuteJobHandler")
    public ReturnT<String> everyDayExecuteJobHandler(String param) {
        log.info("Every day task started");
        if (everyDayExecutes == null || everyDayExecutes.isEmpty()) {
            log.info("No every day tasks registered");
            return new ReturnT<>(200, "SUCCESS");
        }
        for (EveryDayExecute task : everyDayExecutes) {
            try {
                task.execute();
            } catch (Exception e) {
                log.error("Error executing every day task: {}", task.getClass().getSimpleName(), e);
            }
        }
        log.info("Every day task completed");
        return new ReturnT<>(200, "SUCCESS");
    }
}