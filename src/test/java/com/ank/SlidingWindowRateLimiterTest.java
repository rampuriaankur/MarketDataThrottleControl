package com.ank;

import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.ank.util.MyTimer;
import com.ank.util.SlidingWindowRateLimiter;

@RunWith(MockitoJUnitRunner.class)
public class SlidingWindowRateLimiterTest {
    @Mock
    private MyTimer timer;

    @Spy
    @InjectMocks
    private SlidingWindowRateLimiter rateLimiter;

    @Before
    public void before() {
        Mockito.reset(rateLimiter);
    }

    @Test
    public void testOneRequestAtStart() {
        when(timer.getCurrentTime()).thenReturn(1000L);
        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(rateLimiter.isNewDataAllowed());
        }
    }

    @Test
    public void testOneFailedRequest() {
        when(timer.getCurrentTime()).thenReturn(1000L);
        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(rateLimiter.isNewDataAllowed());
        }
        Assert.assertFalse(rateLimiter.isNewDataAllowed());
    }

    @Test
    public void testSlidingAlgo() {
        when(timer.getCurrentTime()).thenReturn(1000L);
        for (int i = 0; i < 84; i++) {
            Assert.assertTrue(rateLimiter.isNewDataAllowed());
        }
        when(timer.getCurrentTime()).thenReturn(2400L);
        for (int i = 0; i < 50; i++) {
            Assert.assertTrue(rateLimiter.isNewDataAllowed());
        }
        Assert.assertFalse(rateLimiter.isNewDataAllowed());
    }

    @Test
    public void testConcurrentUpdateWithoutExceedingRate() throws InterruptedException {
        int numberOfThreads = 5;

        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger failCount = new AtomicInteger(0);
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                when(timer.getCurrentTime()).thenReturn(1000L);
                for (int j = 0; j < 20; j++) {
                    boolean isAllowed = rateLimiter.isNewDataAllowed();
                    if (!isAllowed)
                        failCount.incrementAndGet();
                }
                latch.countDown();
            });
        }
        latch.await();
        service.shutdown();
        Assert.assertEquals(0, failCount.get());
    }

    @Test
    public void testConcurrentUpdateExceedingRate() throws InterruptedException {
        int numberOfThreads = 5;

        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger failCount = new AtomicInteger(0);
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                when(timer.getCurrentTime()).thenReturn(1000L);
                for (int j = 0; j < 21; j++) {
                    boolean isAllowed = rateLimiter.isNewDataAllowed();
                    if (!isAllowed)
                        failCount.incrementAndGet();
                }
                latch.countDown();
            });
        }
        latch.await();
        service.shutdown();
        Assert.assertEquals(5, failCount.get());
    }

    /**
     * Thread 1: 1000*20 --> 2200*10 [0.8*100+0.2*COUNT]
     * Thread 2: 1200*20 --> 2200*10 [0.8*100+0.2*COUNT]
     * Thread 3: 1400*20 --> 3200*10 [0.8*100+0.2*COUNT]
     * Thread 4: 1600*20 --> 3200*10 [0.8*100+0.2*COUNT]
     * Thread 5: 1800*20 --> 3200*10 [0.8*100+0.2*COUNT]
     *
     * @throws InterruptedException
     */
    @Test
    public void testConcurrentUpdateWindowSlidingSpecificCase() throws InterruptedException {
        int numberOfThreads = 5;

        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch1 = new CountDownLatch(numberOfThreads);
        CountDownLatch latch2 = new CountDownLatch(3);
        CountDownLatch latch3 = new CountDownLatch(2);
        AtomicInteger failCount = new AtomicInteger(0);
        for (int i = 0; i < numberOfThreads; i++) {
            int finalI = i;
            service.submit(() -> {
                when(timer.getCurrentTime()).thenReturn(1000L+200* finalI);
                for (int j = 0; j < 20; j++) {
                    boolean isAllowed = rateLimiter.isNewDataAllowed();
                    if (!isAllowed)
                        failCount.incrementAndGet();
                }
                latch1.countDown();
            });
        }
        latch1.await();
        for (int i = 0; i < 3; i++) {
            service.submit(() -> {
                when(timer.getCurrentTime()).thenReturn(2200L);
                for (int j = 0; j < 10; j++) {
                    boolean isAllowed = rateLimiter.isNewDataAllowed();
                    if (!isAllowed)
                        failCount.incrementAndGet();
                }
                latch2.countDown();
            });
        }
        latch2.await();
        for (int i = 0; i < 2; i++) {
            service.submit(() -> {
                when(timer.getCurrentTime()).thenReturn(3200L);
                for (int j = 0; j < 10; j++) {
                    boolean isAllowed = rateLimiter.isNewDataAllowed();
                    if (!isAllowed)
                        failCount.incrementAndGet();
                }
                latch3.countDown();
            });
        }
        latch3.await();
        service.shutdown();
        Assert.assertEquals(10, failCount.get());
    }
}
