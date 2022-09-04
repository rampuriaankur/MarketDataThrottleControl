package com.ank.processor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ank.model.MarketData;
import com.ank.model.SymbolLatestUpdateHistory;
import com.ank.util.MyTimer;
import com.ank.util.SlidingWindowRateLimiter;

/**
 -> Implementation of Market Data Processor with the following requirements:
    Ensure that the number of calls of publishAggregatedMarketData method
    for publishing messages does not exceed 100 times per second, where this
   period is a sliding window
 -> Ensure that each symbol does not update more than once per sliding
    window.
 -> Ensure that each symbol always has the latest market data published.
 -> Ensure the latest market data on each symbol will be published
 */

public class MarketDataProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(MarketDataProcessor.class);

	private final Map<String, SymbolLatestUpdateHistory> symbolLastUpdateMap;

	private final SlidingWindowRateLimiter windowRateLimiter;

	private final MyTimer myTimer;

	public MarketDataProcessor(final MyTimer myTimer) {
		this.windowRateLimiter = new SlidingWindowRateLimiter(myTimer);
		this.myTimer = myTimer;
		this.symbolLastUpdateMap = new ConcurrentHashMap<>();
	}


	public void onMessage(MarketData data) {

		if (windowRateLimiter.isNewDataAllowed()) {
			if (isSymbolAllowed(data)) {
				LOG.debug("Allowed {}", System.currentTimeMillis());
				publishAggregatedMarketData(data);
			}
		}
		// TODO handle rejected cases..
	}

	/**
	  this method allowed only latest market data and check if symbol has been processed within [T:T-1]
	 */
	public boolean isSymbolAllowed(final MarketData data) {
		synchronized (symbolLastUpdateMap) {
			SymbolLatestUpdateHistory history = symbolLastUpdateMap.get(data.getSymbol());
			if (history == null || (myTimer.getCurrentTime() - history.getSystemProcessTime()  > 1000
					&& data.getUpdateTime() > history.getMarketUpdateTime())) {
				symbolLastUpdateMap.put(data.getSymbol(),
						new SymbolLatestUpdateHistory(data.getSymbol(), data.getUpdateTime(), myTimer.getCurrentTime()));
				return true;
			}

			return false;
		}
	}

	// Publish aggregated and throttled market data
	public void publishAggregatedMarketData(MarketData data) {
		//TODO
	}
}
