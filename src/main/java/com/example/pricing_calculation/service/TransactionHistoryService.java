package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.PaymentMethod;
import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.domain.TransactionStatus;
import com.example.pricing_calculation.domain.TransactionType;
import com.example.pricing_calculation.dto.PageResponse;
import com.example.pricing_calculation.dto.TransactionHistoryCreateRequest;
import com.example.pricing_calculation.dto.TransactionHistoryResponse;
import com.example.pricing_calculation.dto.TransactionHistorySummaryResponse;
import com.example.pricing_calculation.dto.TransactionHistoryUpdateRequest;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionHistoryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id",
            "referenceCode",
            "status",
            "gateway",
            "payment.paymentTime",
            "payment.amount"
    );

    private final TransactionHistoryRepository repository;
    private final PaymentRepository paymentRepository;

    public TransactionHistoryService(
            TransactionHistoryRepository repository,
            PaymentRepository paymentRepository) {
        this.repository = repository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public TransactionHistoryResponse create(TransactionHistoryCreateRequest request) {
        validateCreateRequest(request);
        String referenceCode = firstText(request.getTransactionCode(), request.getGatewayReference());
        if (hasText(referenceCode) && repository.existsByReferenceCodeIgnoreCase(referenceCode.trim())) {
            throw new BadRequestException("Reference code already exists");
        }

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + request.getPaymentId()));

        TransactionHistory transaction = new TransactionHistory();
        transaction.setPayment(payment);
        transaction.setReferenceCode(referenceCode);
        transaction.setGateway(firstText(request.getGateway(), request.getGatewayReference(), payment.getPaymentMethod()));
        transaction.setStatus(request.getStatus() == null ? TransactionStatus.PENDING.name() : request.getStatus().name());

        try {
            return TransactionHistoryResponse.from(repository.save(transaction));
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Transaction data violates SmartParking database constraints");
        }
    }

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getById(Long id) {
        return TransactionHistoryResponse.from(findEntity(id));
    }

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getByCode(String transactionCode) {
        TransactionHistory transaction = repository.findByReferenceCodeIgnoreCase(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionCode));
        return TransactionHistoryResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionHistoryResponse> search(
            String keyword,
            TransactionType type,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            String licensePlate,
            String reservationCode,
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        PageRequest pageRequest = buildPageRequest(page, size, sortBy, sortDirection);
        Specification<TransactionHistory> specification = buildSpecification(
                keyword,
                type,
                status,
                paymentMethod,
                licensePlate,
                reservationCode,
                from,
                to,
                minAmount,
                maxAmount
        );
        Page<TransactionHistoryResponse> responsePage = repository.findAll(specification, pageRequest)
                .map(TransactionHistoryResponse::from);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return repository.findRecent(PageRequest.of(0, safeLimit)).stream()
                .map(TransactionHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionHistorySummaryResponse summary(LocalDateTime from, LocalDateTime to) {
        Specification<TransactionHistory> specification = buildSpecification(
                null,
                null,
                null,
                null,
                null,
                null,
                from,
                to,
                null,
                null
        );
        List<TransactionHistory> transactions = repository.findAll(specification);
        long total = transactions.size();
        long completed = transactions.stream().filter(this::isCompleted).count();
        long pending = transactions.stream().filter(this::isPending).count();
        long refunded = transactions.stream().filter(transaction -> equalsStatus(transaction.getStatus(), "REFUNDED")).count();
        long failed = transactions.stream().filter(transaction -> equalsStatus(transaction.getStatus(), "FAILED")).count();
        long cancelled = transactions.stream().filter(transaction -> equalsStatus(transaction.getStatus(), "CANCELLED")).count();
        BigDecimal totalRevenue = transactions.stream()
                .filter(this::isCompleted)
                .map(TransactionHistory::getPayment)
                .filter(payment -> payment != null && payment.getAmount() != null)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageAmount = completed == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);

        return new TransactionHistorySummaryResponse(
                total,
                completed,
                pending,
                cancelled,
                failed,
                refunded,
                transactions.stream().filter(transaction -> transaction.getPayment() != null
                        && transaction.getPayment().getSession() != null
                        && transaction.getPayment().getSession().getReservation() != null).count(),
                total,
                transactions.stream().filter(transaction -> transaction.getPayment() != null
                        && transaction.getPayment().getSession() != null).count(),
                0,
                totalRevenue,
                averageAmount
        );
    }

    @Transactional
    public TransactionHistoryResponse update(Long id, TransactionHistoryUpdateRequest request) {
        TransactionHistory transaction = findEntity(id);
        validateUpdateRequest(request);

        if (request.getGateway() != null) {
            transaction.setGateway(request.getGateway());
        }
        if (request.getGatewayReference() != null) {
            transaction.setReferenceCode(request.getGatewayReference());
        }
        if (request.getStatus() != null) {
            transaction.setStatus(request.getStatus().name());
        }

        return TransactionHistoryResponse.from(repository.save(transaction));
    }

    @Transactional
    public TransactionHistoryResponse changeStatus(Long id, TransactionStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        TransactionHistory transaction = findEntity(id);
        transaction.setStatus(status.name());
        return TransactionHistoryResponse.from(repository.save(transaction));
    }

    @Transactional
    public void delete(Long id) {
        TransactionHistory transaction = findEntity(id);
        repository.delete(transaction);
    }

    private TransactionHistory findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    private PageRequest buildPageRequest(int page, int size, String sortBy, String sortDirection) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        String requestedSort = mapSortField(sortBy);
        String safeSortBy = SORTABLE_FIELDS.contains(requestedSort) ? requestedSort : "payment.paymentTime";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    private String mapSortField(String sortBy) {
        if ("transactionCode".equals(sortBy)) {
            return "referenceCode";
        }
        if ("occurredAt".equals(sortBy)) {
            return "payment.paymentTime";
        }
        if ("amount".equals(sortBy)) {
            return "payment.amount";
        }
        return sortBy;
    }

    private Specification<TransactionHistory> buildSpecification(
            String keyword,
            TransactionType type,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            String licensePlate,
            String reservationCode,
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> payment = root.join("payment", JoinType.LEFT);
            Join<Object, Object> session = payment.join("session", JoinType.LEFT);
            Join<Object, Object> vehicle = session.join("vehicle", JoinType.LEFT);
            Join<Object, Object> user = vehicle.join("user", JoinType.LEFT);
            Join<Object, Object> vehicleType = vehicle.join("vehicleType", JoinType.LEFT);
            Join<Object, Object> slot = session.join("slot", JoinType.LEFT);
            Join<Object, Object> zone = slot.join("zone", JoinType.LEFT);

            if (hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("referenceCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("gateway")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(payment.get("paymentMethod")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(session.get("ticketCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(vehicle.get("plateNumber")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(vehicleType.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("phone")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(slot.get("slotCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(zone.get("zoneName")), pattern)
                ));
            }
            if (type != null) {
                predicates.add(typePredicate(type, root, payment, session, criteriaBuilder));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), status.name()));
            }
            if (paymentMethod != null) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(payment.get("paymentMethod")), paymentMethod.name()));
            }
            if (hasText(licensePlate)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(vehicle.get("plateNumber")),
                        "%" + licensePlate.trim().toLowerCase() + "%"
                ));
            }
            if (hasText(reservationCode)) {
                Long reservationId = parseLong(reservationCode);
                if (reservationId != null) {
                    Join<Object, Object> reservation = session.join("reservation", JoinType.LEFT);
                    predicates.add(criteriaBuilder.equal(reservation.get("id"), reservationId));
                }
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(payment.get("paymentTime"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(payment.get("paymentTime"), to));
            }
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(payment.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(payment.get("amount"), maxAmount));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate typePredicate(
            TransactionType type,
            jakarta.persistence.criteria.Root<TransactionHistory> root,
            Join<Object, Object> payment,
            Join<Object, Object> session,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder) {
        return switch (type) {
            case PAYMENT_COMPLETED -> criteriaBuilder.or(
                    criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), TransactionStatus.COMPLETED.name()),
                    criteriaBuilder.equal(criteriaBuilder.upper(payment.get("status")), TransactionStatus.COMPLETED.name()),
                    criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), "SUCCESS"),
                    criteriaBuilder.equal(criteriaBuilder.upper(payment.get("status")), "SUCCESS")
            );
            case PAYMENT_PENDING -> criteriaBuilder.or(
                    criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), TransactionStatus.PENDING.name()),
                    criteriaBuilder.equal(criteriaBuilder.upper(payment.get("status")), TransactionStatus.PENDING.name())
            );
            case REFUND_CREATED -> criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), TransactionStatus.REFUNDED.name());
            case CHECKOUT_COMPLETED -> criteriaBuilder.or(
                    criteriaBuilder.equal(criteriaBuilder.upper(session.get("status")), "CHECKED_OUT"),
                    criteriaBuilder.equal(criteriaBuilder.upper(session.get("status")), "CLOSED")
            );
            case RESERVATION_CREATED -> criteriaBuilder.isNotNull(session.get("reservation"));
            default -> criteriaBuilder.conjunction();
        };
    }

    private void validateCreateRequest(TransactionHistoryCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Transaction request is required");
        }
        if (request.getPaymentId() == null) {
            throw new BadRequestException("paymentId is required because SmartParking.Transactions references Payments.payment_id");
        }
        validateReferenceCode(firstText(request.getTransactionCode(), request.getGatewayReference()));
    }

    private void validateUpdateRequest(TransactionHistoryUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Transaction update request is required");
        }
        validateReferenceCode(request.getGatewayReference());
    }

    private void validateReferenceCode(String referenceCode) {
        if (referenceCode != null && referenceCode.length() > 100) {
            throw new BadRequestException("Reference code cannot exceed 100 characters");
        }
    }

    private boolean isCompleted(TransactionHistory transaction) {
        Payment payment = transaction.getPayment();
        return equalsStatus(transaction.getStatus(), "COMPLETED")
                || equalsStatus(transaction.getStatus(), "SUCCESS")
                || payment != null && (equalsStatus(payment.getStatus(), "COMPLETED")
                || equalsStatus(payment.getStatus(), "SUCCESS"));
    }

    private boolean isPending(TransactionHistory transaction) {
        Payment payment = transaction.getPayment();
        return equalsStatus(transaction.getStatus(), "PENDING")
                || payment != null && equalsStatus(payment.getStatus(), "PENDING");
    }

    private boolean equalsStatus(String value, String expected) {
        return value != null && value.trim().equalsIgnoreCase(expected);
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
