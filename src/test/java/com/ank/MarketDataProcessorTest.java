package com.ank;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javafaker.Faker;
import com.ank.model.MarketData;
import com.ank.processor.MarketDataProcessor;
import com.ank.util.MyTimer;
import com.ank.util.SlidingWindowRateLimiter;

@RunWith(MockitoJUnitRunner.class)
public final class MarketDataProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    @Mock
    private MyTimer timer;

    @Spy
    @InjectMocks
    private SlidingWindowRateLimiter rateLimiter;

    @Spy
    @InjectMocks
    private MarketDataProcessor marketDataProcessor;

    @Before
    public void before() {
        Mockito.reset(rateLimiter,marketDataProcessor);
    }

    @Test
    public void testPublishAHundredPerSecond(){
        // seed that constantly produce 100 distinct symbols
        Faker faker = new Faker(new Random(9));
        when(timer.getCurrentTime()).thenReturn(1000L);
        for (int i = 0; i < 100; i++) {
            marketDataProcessor.onMessage(getDummyMarketData(faker.stock().nsdqSymbol(), 1000));
        }
        verify(marketDataProcessor, times(100)).publishAggregatedMarketData(any());
    }

    @Test
    public void testConcurrentPublishMoreThanHundredPerSecond() throws InterruptedException {
        int numberOfThreads = 5;

        Faker faker = new Faker(new Random(9));
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                when(timer.getCurrentTime()).thenReturn(1000L);
                for (int j = 0; j < 21; j++) {
                    marketDataProcessor.onMessage(getDummyMarketData(faker.stock().nsdqSymbol(), 1000));
                }
                latch.countDown();
            });
        }
        latch.await();
        service.shutdown();
        verify(marketDataProcessor, times(100)).publishAggregatedMarketData(any());
    }

    @Test
    public void testPublishMoreThanHundredPerSecond(){
        Faker faker = new Faker(new Random(9));
        when(timer.getCurrentTime()).thenReturn(1620664506540L);
        for (int i = 0; i < 100; i++) {
            marketDataProcessor.onMessage(getDummyMarketData(faker.stock().nsdqSymbol(), 500));
        }
        verify(marketDataProcessor, times(100)).publishAggregatedMarketData(any());
        when(timer.getCurrentTime()).thenReturn(1620664506640L);
        for (int i = 0; i < 100; i++) {
            marketDataProcessor.onMessage(getDummyMarketData(faker.stock().nsdqSymbol(), 1000));
        }
        verify(marketDataProcessor, times(100)).publishAggregatedMarketData(any());
        when(timer.getCurrentTime()).thenReturn(1620664506740L);
        for (int i = 0; i < 100; i++) {
            marketDataProcessor.onMessage(getDummyMarketData(faker.stock().nsdqSymbol(), 1500));
        }
        when(timer.getCurrentTime()).thenReturn(1620664506840L);
        for (int i = 0; i < 100; i++) {
            marketDataProcessor.onMessage(getDummyMarketData(faker.stock().nsdqSymbol(), 2000));
        }
        verify(marketDataProcessor, times(100)).publishAggregatedMarketData(any());
    }

    @Test
    public void testSymbolNotHaveMoreThanOneUpdatePerSecond(){
        when(timer.getCurrentTime()).thenReturn(1000L);
        for (int i = 0; i < 100; i++) {
            marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1000+i));
        }
        verify(marketDataProcessor, times(1)).publishAggregatedMarketData(any());
    }

	/**
	 * At an earlier point in time, processed MSFT with updated time as
	 * 1620664496540. 1s later and 2s later another 2 events for MSFT with
	 * updated time 1620664496440 and 1620664496430 (both are outdated) came
	 * respectively.
	 */
	@Test
	public void testSymbolAlwaysHaveTheLatestDataWhenPublishTwoOutdatedData() {
		when(timer.getCurrentTime()).thenReturn(1620664506540L);
		marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1620664496540L));
		when(timer.getCurrentTime()).thenReturn(1620664507540L);
		marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1620664496440L));
		when(timer.getCurrentTime()).thenReturn(1620664508540L);
		marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1620664496430L));
		verify(marketDataProcessor, times(1)).publishAggregatedMarketData(any());
	}

    /**
     * At an earlier point in time, processed MSFT with updated time as
     * 1620664496540. 1s later and 2s later another 2 events for MSFT with
     * updated time 1620664496440 and 1620664496430 (one is outdated) came respectively.
     */
    @Test
    public void testSymbolAlwaysHaveTheLatestDataWhenPublishOneOutdatedData() {
        when(timer.getCurrentTime()).thenReturn(1620664506540L);
        marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1620664496540L));
        when(timer.getCurrentTime()).thenReturn(1620664507540L);
        marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1620664496440L));
        when(timer.getCurrentTime()).thenReturn(1620664508540L);
        marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1620664496840L));
        verify(marketDataProcessor, times(2)).publishAggregatedMarketData(any());
    }


	/**
     * Faker with the seed will provide the same in-order symbols on every
     * request under each thread, namely:
     *
     * Thread 1:
     * MSFT 1620664496540 [arrival time]
     * TSLA 1620664496560
     * APPL 1620664496580
     *
     * Thread 2:
     * MSFT 1620664496560
     * TSLA 1620664496580
     * APPL 1620664496600
     *
     * Thread 3:
     * MSFT 1620664496580
     * TSLA 1620664496600
     * APPL 1620664496620
     *
     * Thread 4:
     * MSFT 1620664496600
     * TSLA 1620664496620
     * APPL 1620664496640
     *
     * @throws InterruptedException
     */
    @Test
    public void testConcurrentPublishMoreThanOneUpdatePerSecond() throws InterruptedException {
        int numberOfThreads = 5;

        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads*3);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            Faker faker = new Faker(new Random(9));
            int finalI = i;
            service.submit(() -> {
                for (int j = 0; j < 50; j++) {
                    int finalJ = j;
                    String symbol = faker.stock().nsdqSymbol();
                    when(timer.getCurrentTime()).thenReturn((1620664496540L+ finalJ*20+ finalI *20));
                    LOG.error(symbol + ": " + (1620664496540L+ finalJ*20+ finalI *20));
                    marketDataProcessor.onMessage(getDummyMarketData(symbol, 2000L+ finalJ*20));
                }
                latch.countDown();
            });
        }
        latch.await();
        service.shutdown();
        verify(marketDataProcessor, times(50)).publishAggregatedMarketData(any());
    }
//
//    /**
//     *
//     * @throws InterruptedException
//     */
//    @Test
//    public void testConcurrentComplexScenario() throws InterruptedException {
//        int numberOfThreads = 3;
//
//        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
//        CountDownLatch latch = new CountDownLatch(numberOfThreads);
//        service.submit(() -> {
//            for (int j = 0; j < 500; j++) {
//                when(timer.getCurrentTime()).thenReturn(1620664496540L+ j*20);
//                marketDataProcessor.onMessage(getDummyMarketData("MSFT", 1620664496340L+ j*20));
//            }
//            latch.countDown();
//        });
//        service.submit(() -> {
//            for (int j = 0; j < 300; j++) {
//                when(timer.getCurrentTime()).thenReturn(1620664496530L+ j*30);
//                marketDataProcessor.onMessage(getDummyMarketData("APPL", 1620664496340L+ j*20));
//            }
//            latch.countDown();
//        });
//        service.submit(() -> {
//            for (int j = 0; j < 200; j++) {
//                when(timer.getCurrentTime()).thenReturn(1620664496550L+ j*50);
//                marketDataProcessor.onMessage(getDummyMarketData("TSLA", 1620664496340L+ j*20));
//            }
//            latch.countDown();
//        });
//        latch.await();
//        service.shutdown();
//        verify(marketDataProcessor, times(50)).publishAggregatedMarketData(any());
//    }
//

    private MarketData getDummyMarketData(final String symbol, final long updateTime){
        return new MarketData(symbol, BigDecimal.ONE, BigDecimal.ONE,BigDecimal.ONE, updateTime);
    }
}
