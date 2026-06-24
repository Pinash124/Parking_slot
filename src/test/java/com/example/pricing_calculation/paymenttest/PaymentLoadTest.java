package com.example.pricing_calculation.paymenttest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.show-sql=false"
)
@EnabledIfSystemProperty(named = "payment.load", matches = "true")
class PaymentLoadTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentTestDataFactory dataFactory;
    private String runPrefix;

    @BeforeEach
    void setUp() {
        dataFactory = new PaymentTestDataFactory(jdbcTemplate);
        runPrefix = PaymentTestDataFactory.newRunPrefix("L");
        dataFactory.cleanup(runPrefix);
    }

    @AfterEach
    void cleanUp() {
        dataFactory.cleanup(runPrefix);
        assertEquals(0, dataFactory.countMarkers(runPrefix), "Load test data must be cleaned up");
    }

    @Test
    void concurrentPaymentBotsCompleteWithinConfiguredSlo() throws Exception {
        int users = Integer.getInteger("payment.load.users", 30);
        int concurrency = Integer.getInteger("payment.load.concurrency", 10);
        long p95LimitMillis = Long.getLong("payment.load.p95-ms", 15000L);
        assertTrue(users > 0 && users <= 200, "payment.load.users must be between 1 and 200");
        assertTrue(concurrency > 0 && concurrency <= users, "concurrency must be between 1 and users");

        List<PaymentBotSeed> seeds = new ArrayList<>();
        for (int index = 0; index < users; index++) {
            seeds.add(dataFactory.createSeed(runPrefix, index + 1));
        }

        PaymentFlowBot bot = new PaymentFlowBot("http://localhost:" + port, objectMapper);
        String[] gateways = {"CASH", "MOMO", "VNPAY"};
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<PaymentFlowResult>> futures = new ArrayList<>();
        long startedAt = System.nanoTime();
        try {
            for (int index = 0; index < users; index++) {
                PaymentBotSeed seed = seeds.get(index);
                String gateway = gateways[index % gateways.length];
                Callable<PaymentFlowResult> task = () -> {
                    startGate.await();
                    return bot.run(seed, gateway);
                };
                futures.add(executor.submit(task));
            }
            startGate.countDown();
            List<PaymentFlowResult> results = new ArrayList<>();
            for (Future<PaymentFlowResult> future : futures) {
                results.add(future.get(90, TimeUnit.SECONDS));
            }

            long totalMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            List<Long> durations = results.stream()
                    .map(PaymentFlowResult::durationMillis)
                    .sorted(Comparator.naturalOrder())
                    .toList();
            long p95Millis = durations.get(Math.max(0, (int) Math.ceil(durations.size() * 0.95) - 1));
            long successful = results.stream().filter(PaymentFlowResult::successful).count();
            double flowsPerSecond = successful * 1000.0 / Math.max(1, totalMillis);

            results.stream()
                    .filter(result -> !result.successful())
                    .forEach(result -> System.out.printf(
                            "[payment-load-error] bot=%s gateway=%s error=%s%n",
                            result.botCode(), result.gateway(), result.error()));
            System.out.printf(
                    "[payment-load] users=%d concurrency=%d successful=%d totalMs=%d p95Ms=%d flowsPerSecond=%.2f%n",
                    users, concurrency, successful, totalMillis, p95Millis, flowsPerSecond);

            assertEquals(users, successful, "Every payment bot must complete successfully");
            assertTrue(
                    p95Millis <= p95LimitMillis,
                    () -> "p95 " + p95Millis + "ms exceeded limit " + p95LimitMillis + "ms"
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
