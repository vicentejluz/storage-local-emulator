package com.vicente.storage.scheduler.util;

import com.vicente.storage.domain.enums.MasterKeyStatus;
import com.vicente.storage.repository.MasterKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MasterKeySchedulerHelper {
    private final MasterKeyRepository masterKeyRepository;
    private static final Logger logger = LoggerFactory.getLogger(MasterKeySchedulerHelper.class);

    public MasterKeySchedulerHelper(MasterKeyRepository masterKeyRepository) {
        this.masterKeyRepository = masterKeyRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transitionRotatingToInactive(Long rotatingVersionCandidate) {
        logger.info("[MASTER KEY SCHEDULER] Transitioning MasterKey version={} from ROTATING to INACTIVE",
                rotatingVersionCandidate);
        masterKeyRepository.transitionStatus(rotatingVersionCandidate,
                MasterKeyStatus.ROTATING, MasterKeyStatus.INACTIVE);
        logger.info("[MASTER KEY SCHEDULER] MasterKey version={} transitioned successfully to INACTIVE",
                rotatingVersionCandidate);
    }
}
