**Assumptions**

* The sliding windows rate limiter will NOT count rejected requests into the current window.
* Only ONE market data processor will process each particular set of market data, 
  ie no two processors will be subscribing to the price update of the same symbol.
* If an request is rejected either by the rate limiter check or the symbol check, it will be ignored for now.
* The algorithm used: 
  > Used the approximate algorithm to calculate the number of requests within that window
  > R(t) = Rp x (1000 - timeElapsedInCurrentWindow)/1000 + Rc should be < 100                                  
* Upon calling `publishAggregatedMarketData`, market data of that symbol should carry the latest info, which is 
  dictated by the field `updateTime`

