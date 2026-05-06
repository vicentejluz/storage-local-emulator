package com.vicente.storage.scheduler;

import com.vicente.storage.scheduler.service.AccessKeySchedulerService;
import com.vicente.storage.scheduler.service.MasterKeySchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class KeyRotationScheduler {
    private final AccessKeySchedulerService accessKeySchedulerService;
    private final MasterKeySchedulerService masterKeySchedulerService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(KeyRotationScheduler.class);

    public KeyRotationScheduler(AccessKeySchedulerService accessKeySchedulerService, MasterKeySchedulerService masterKeySchedulerService) {
        this.accessKeySchedulerService = accessKeySchedulerService;
        this.masterKeySchedulerService = masterKeySchedulerService;
    }


    @Scheduled(cron = "${security.rotation.cron:10 5 1 * * *}")
    public void runScheduled() {
        execute();
    }

    private void execute() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("[KEY ROTATION SCHEDULER] Execution skipped — already running");
            return;
        }

        long start = System.currentTimeMillis();

        logger.info("[KEY ROTATION SCHEDULER] Task started | operation=keyRotationCycle");

        try {

            accessKeySchedulerService.rotatePendingAccessKeys();
            masterKeySchedulerService.finalizeCompletedRotations();

            long duration = System.currentTimeMillis() - start;
            logger.info("[KEY ROTATION SCHEDULER] Task finished successfully | duration={}ms", duration);
        } catch (Exception e){
            long duration = System.currentTimeMillis() - start;
            logger.error("[KEY ROTATION SCHEDULER] Task failed | duration={}ms | cause={}", duration, e.getMessage(), e);
        }finally {
            running.set(false);
            logger.debug("[KEY ROTATION SCHEDULER] Execution lock released");
        }
    }
}
