package com.vicente.storage.security.validation;

import com.vicente.storage.exception.InvalidTimestampException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RequestValidator {
    private final Duration timestampSkew;
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    public RequestValidator(@Value("${security.request.timestamp-skew}") Duration timestampSkew) {
        this.timestampSkew = timestampSkew;
    }

    public void validateTimestamp(Instant timestamp){
    Instant now = Instant.now();
    Instant min = now.minus(timestampSkew);
    Instant max = now.plus(timestampSkew);

        if(timestamp.isBefore(min) || timestamp.isAfter(max)) {
            logger.warn("Request timestamp outside allowed window. requestTimestamp={}, serverNow={}, minAllowed={}, maxAllowed={}",
                    timestamp, now, min, max);
            throw new InvalidTimestampException("Request timestamp is outside the allowed window. Please check your system clock.");
        }
    }
}
