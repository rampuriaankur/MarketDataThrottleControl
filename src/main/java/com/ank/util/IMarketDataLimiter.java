package com.ank.util;

/**
 * Interface for Rate Limiter
 */
public interface IMarketDataLimiter {
    boolean isNewDataAllowed();
}
