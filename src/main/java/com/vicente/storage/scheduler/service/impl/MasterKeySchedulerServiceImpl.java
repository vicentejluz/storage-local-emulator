package com.vicente.storage.scheduler.service.impl;

import com.vicente.storage.repository.AccessKeyRepository;
import com.vicente.storage.repository.MasterKeyRepository;
import com.vicente.storage.scheduler.service.MasterKeySchedulerService;
import com.vicente.storage.scheduler.util.MasterKeySchedulerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MasterKeySchedulerServiceImpl implements MasterKeySchedulerService {
    private final AccessKeyRepository accessKeyRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final MasterKeySchedulerHelper masterKeySchedulerHelper;
    private static final Logger logger = LoggerFactory.getLogger(MasterKeySchedulerServiceImpl.class);

    public MasterKeySchedulerServiceImpl(AccessKeyRepository accessKeyRepository,
                                         MasterKeyRepository masterKeyRepository, MasterKeySchedulerHelper masterKeySchedulerHelper) {
        this.accessKeyRepository = accessKeyRepository;
        this.masterKeyRepository = masterKeyRepository;
        this.masterKeySchedulerHelper = masterKeySchedulerHelper;
    }

    @Override
    public void finalizeCompletedRotations() {
        logger.debug("[MASTER KEY SCHEDULER] Starting completion check for ROTATING MasterKey(s)");
        List<Long> rotatingVersionCandidates = masterKeyRepository.findRotatingVersions();
        evaluateRotatingMasterKeysForInactivation(rotatingVersionCandidates);
    }

    private void evaluateRotatingMasterKeysForInactivation(List<Long> rotatingVersionCandidates) {
        if (!rotatingVersionCandidates.isEmpty()) {
            AtomicInteger success = new AtomicInteger();
            AtomicInteger failed = new AtomicInteger();
            logger.info("[MASTER KEY SCHEDULER] Found {} ROTATING MasterKey version(s) to evaluate",
                    rotatingVersionCandidates.size());
            processRotatingMasterKeyCandidates(rotatingVersionCandidates, success, failed);
            if (success.get() > 0) {
                logger.info("[MASTER KEY SCHEDULER] Completion check finished | transitioned={} failed={} total={}",
                        success.get(), failed.get(), rotatingVersionCandidates.size()

                );
                return;
            }
            logger.warn("[MASTER KEY SCHEDULER] Completion check finished with no successful transitions | failed={} total={}",
                    failed.get(), rotatingVersionCandidates.size());
        } else {
            logger.debug("[MASTER KEY SCHEDULER] No ROTATING MasterKey(s) found");
        }
    }

    private void processRotatingMasterKeyCandidates(List<Long> rotatingVersionCandidates, AtomicInteger success, AtomicInteger failed) {
        rotatingVersionCandidates.forEach(rotatingVersion -> {
            try {
                logger.debug("[MASTER KEY SCHEDULER] Checking MasterKey version={} for pending AccessKey references",
                        rotatingVersion);
                if (accessKeyRepository.countByMasterKeyVersion(rotatingVersion).equals(0)) {
                    masterKeySchedulerHelper.transitionRotatingToInactive(rotatingVersion);
                    success.incrementAndGet();
                }
            } catch (DataAccessException e) {
                failed.getAndIncrement();
                logger.warn("[MASTER KEY SCHEDULER] Failed processing MasterKey version={} | cause={}",
                        rotatingVersion, e.getMessage(), e);
            }
        });
    }
}
