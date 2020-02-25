package com.sap.cloud.lm.sl.cf.client.util;

import java.text.MessageFormat;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.common.util.MiscUtil;

public class ResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientOperationExecutor.class);

    private static final long DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS = 5000;

    private long retryCount = DEFAULT_RETRY_COUNT;
    private long waitTimeBetweenRetriesInMillis = DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS;

    public ResilientOperationExecutor withRetryCount(long retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public ResilientOperationExecutor withWaitTimeBetweenRetriesInMillis(long waitTimeBetweenRetriesInMillis) {
        this.waitTimeBetweenRetriesInMillis = waitTimeBetweenRetriesInMillis;
        return this;
    }

    public void execute(Runnable operation) {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    public <T> T execute(Supplier<T> operation) {
        for (int i = 1; i < retryCount; i++) {
            try {
                return operation.get();
            } catch (RuntimeException e) {
                handle(e);
                MiscUtil.sleep(waitTimeBetweenRetriesInMillis);
            }
        }
        return operation.get();
    }

    protected void handle(RuntimeException e) {
        LOGGER.warn(MessageFormat.format("Retrying operation that failed with message: {0}", e.getMessage()), e);
    }

}
