package com.example.pricing_calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.domain.TransactionStatus;
import com.example.pricing_calculation.dto.TransactionHistoryCreateRequest;
import com.example.pricing_calculation.dto.TransactionHistoryResponse;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import com.example.pricing_calculation.service.BadRequestException;
import com.example.pricing_calculation.service.ResourceNotFoundException;
import com.example.pricing_calculation.service.TransactionHistoryService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PricingCalculationApplicationTests {

    private final TransactionHistoryRepository transactionRepository = mock(TransactionHistoryRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final TransactionHistoryService service = new TransactionHistoryService(
            transactionRepository,
            paymentRepository
    );

    @Test
    void createRejectsMissingPaymentIdForSmartParkingSchema() {
        TransactionHistoryCreateRequest request = new TransactionHistoryCreateRequest();
        request.setTransactionCode("TXN-001");

        assertThrows(BadRequestException.class, () -> service.create(request));
    }

    @Test
    void createRejectsUnknownPaymentId() {
        TransactionHistoryCreateRequest request = new TransactionHistoryCreateRequest();
        request.setPaymentId(99L);
        request.setTransactionCode("TXN-001");
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(request));
    }

    @Test
    void changeStatusUpdatesTransactionStatus() {
        TransactionHistory transaction = new TransactionHistory();
        when(transactionRepository.findById(7L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(TransactionHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionHistoryResponse response = service.changeStatus(7L, TransactionStatus.REFUNDED);

        assertEquals("REFUNDED", response.getStatus());
        verify(transactionRepository).save(transaction);
    }
}
