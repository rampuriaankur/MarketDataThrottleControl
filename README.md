**Requirements**


* There is a MarketDataProcessor class below. This class receives real-time market data
from the exchange and then publishes them to other applications.
* MarketDataProcessor receives MarketData from a source through the onMessage
method. There is a single thread that calls onMessage at an unknown rate per second.
* Modify the MarketDataProcessor class to,
  * At least fulfill,
    * Ensure that the number of calls of publishAggregatedMarketData method
for publishing messages does not exceed 100 times per second, where this
period is a sliding window.
    * Ensure that each symbol does not update more than once per sliding
window.
  * Prefer to fulfill,
    * Ensure that each symbol always has the latest market data published.
    * Ensure the latest market data on each symbol will be published.
* The MarketData class contains the symbol, price and update time. The data types are
determined yourself



**Assumptions**

* The sliding windows rate limiter will NOT count rejected requests into the current window.
* Only ONE market data processor will process each particular set of market data, 
  ie no two processors will be subscribing to the price update of the same symbol.
* If an request is rejected either by the rate limiter check or the symbol check, it will be ignored for now.
* The algorithm used: 
  > Used the approximate algorithm to calculate the number of requests within that window
  > R(t) = Rp x (1000 - timeElapsedInCurrentWindow)/1000 + Rc should be < 100                                  


