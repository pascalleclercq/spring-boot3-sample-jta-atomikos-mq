package sample.atomikos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;


class AccountServicePerformanceTest extends AbstractMqIntegrationTest {
	
    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

	@JmsListener(destination = "DEV.QUEUE.1", containerFactory = "nonXAJmsListenerContainerFactory")
	public void onMessage(String content) {
		//System.out.println("----> " + content);
	}
    
    @Test
    void performanceTest() throws InterruptedException {
        int threadCount = 30;
    	int callCount = 5000;
    	
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch countLatch = new CountDownLatch(callCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < callCount; i++) {
            final String username = "perf_user_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    accountService.createAccountAndNotify(username);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Call failed for " + username + ": " + e.getMessage());
                } finally {
                	countLatch.countDown();
                }
                return null;
            });
        }

        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        boolean completed = countLatch.await(10, TimeUnit.MINUTES);
        long elapsedMs = System.currentTimeMillis() - startTime;
       
        executor.shutdown();

        System.out.printf("Completed: %b | Duration: %d ms | Throughput: %.1f calls/sec%n",
                completed, elapsedMs, callCount * 1000.0 / elapsedMs);
        System.out.printf("Successes: %d | Errors: %d%n", successCount.get(), errorCount.get());

        assertThat(completed).as("all tasks completed within timeout").isTrue();
        assertThat(errorCount.get()).as("no errors").isZero();
        assertThat(successCount.get()).as("all calls succeeded").isEqualTo(callCount);
        assertThat(accountRepository.count()).as("all accounts persisted").isEqualTo(callCount);
    }

}
