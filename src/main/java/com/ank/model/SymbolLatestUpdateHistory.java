package com.ank.model;

/**
 * Model for storing market last update time and last system processed time for
 * an individual symbol
 */
public class SymbolLatestUpdateHistory {
	private final String symbol;
	private final long marketUpdateTime;
	private final long systemProcessTime;

	public SymbolLatestUpdateHistory(String symbol, long marketUpdateTime, long systemProcessTime) {
		this.symbol = symbol;
		this.marketUpdateTime = marketUpdateTime;
		this.systemProcessTime = systemProcessTime;
	}

	public String getSymbol() {
		return symbol;
	}

	public long getMarketUpdateTime() {
		return marketUpdateTime;
	}

	public long getSystemProcessTime() {
		return systemProcessTime;
	}
}
