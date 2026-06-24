package com.example.pricing_calculation.paymenttest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
@EnabledIfSystemProperty(named = "payment.smoke", matches = "true")
class PaymentFlowSmokeTest {

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
        runPrefix = PaymentTestDataFactory.newRunPrefix("S");
        dataFactory.cleanup(runPrefix);
    }

    @AfterEach
    void cleanUp() {
        dataFactory.cleanup(runPrefix);
        assertEquals(0, dataFactory.countMarkers(runPrefix), "Smoke test data must be cleaned up");
    }

    @Test
    void botsCompleteCashMomoAndVnpayPaymentFlows() {
        PaymentFlowBot bot = new PaymentFlowBot("http://localhost:" + port, objectMapper);
        String[] gateways = {"CASH", "MOMO", "VNPAY"};
        List<PaymentFlowResult> results = new ArrayList<>();
        for (int index = 0; index < gateways.length; index++) {
            results.add(bot.run(dataFactory.createSeed(runPrefix, index + 1), gateways[index]));
        }

        for (PaymentFlowResult result : results) {
            assertTrue(result.successful(), () -> result.gateway() + " failed: " + result.error());
            assertTrue(result.totalFee().signum() > 0);
            assertTrue(result.durationMillis() < 20000);
            System.out.printf(
                    "[payment-smoke] gateway=%s durationMs=%d totalFee=%s paymentId=%s reference=%s%n",
                    result.gateway(),
                    result.durationMillis(),
                    result.totalFee(),
                    result.paymentId(),
                    result.referenceCode()
            );
        }
    }
}
