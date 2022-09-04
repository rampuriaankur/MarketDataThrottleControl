There is a MarketDataProcessor class below. This class receives real-time market data
from the exchange and then publishes them to other applications.
 MarketDataProcessor receives MarketData from a source through the onMessage
method. There is a single thread that calls onMessage at an unknown rate per second.
 Modify the MarketDataProcessor class to,
o At least fulfill,
 Ensure that the number of calls of publishAggregatedMarketData method
for publishing messages does not exceed 100 times per second, where this
period is a sliding window.
 Ensure that each symbol does not update more than once per sliding
window.
o Prefer to fulfill,
 Ensure that each symbol always has the latest market data published.
 Ensure the latest market data on each symbol will be published.
 The MarketData class contains the symbol, price and update time. The data types are
determined yourself
