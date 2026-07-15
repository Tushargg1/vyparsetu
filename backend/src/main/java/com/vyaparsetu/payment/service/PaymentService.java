package com.vyaparsetu.payment.service;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.common.util.NumberGenerator;
import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.payment.dto.PaymentInitRequest;
import com.vyaparsetu.payment.dto.PaymentResponse;
import com.vyaparsetu.payment.entity.Payment;
import com.vyaparsetu.payment.repository.PaymentRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final CreditService creditService;
    private final UserService userService;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                          WalletService walletService, CreditService creditService,
                          UserService userService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.walletService = walletService;
        this.creditService = creditService;
        this.userService = userService;
    }

    @Transactional
    public PaymentResponse init(PaymentInitRequest req) {
        Long retailerId = userService.currentRetailerId();
        Order order = orderRepository.findByIdForUpdate(req.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", req.orderId()));
        if (!order.getRetailerId().equals(retailerId)) {
            throw new AccessDeniedException("Not your order");
        }
        if (req.mode() != Enums.PaymentMode.WALLET && req.mode() != Enums.PaymentMode.CREDIT) {
            throw new BusinessException("PAYMENT_MODE_UNAVAILABLE", HttpStatus.CONFLICT,
                    "This payment mode is not available until provider verification is configured");
        }

        java.math.BigDecimal paid = order.getAmountPaid() == null
                ? java.math.BigDecimal.ZERO : order.getAmountPaid();
        java.math.BigDecimal outstanding = order.getTotalAmount().subtract(paid);
        if (order.getPaymentStatus() == Enums.PaymentStatus.PAID || outstanding.signum() <= 0) {
            throw new BusinessException("PAYMENT_ALREADY_PAID", HttpStatus.CONFLICT,
                    "This order is already fully paid");
        }
        java.util.Optional<Payment> activePayment = paymentRepository
                .findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(order.getId(),
                        java.util.List.of(Payment.PaymentTxnStatus.INITIATED, Payment.PaymentTxnStatus.SUCCESS));
        Payment payment = activePayment.orElseGet(Payment::new);
        if (payment.getStatus() == Payment.PaymentTxnStatus.SUCCESS) {
            throw new BusinessException("PAYMENT_ALREADY_PAID", HttpStatus.CONFLICT,
                    "This order already has a successful payment");
        }
        payment.setOrderId(order.getId());
        payment.setRetailerId(retailerId);
        payment.setMode(req.mode());
        payment.setAmount(outstanding);
        payment.setGateway(gatewayFor(req.mode()));
        payment.setStatus(Payment.PaymentTxnStatus.INITIATED);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse confirm(String paymentUuid, String gatewayRef) {
        Payment initial = paymentRepository.findByUuid(paymentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentUuid));
        if (!initial.getRetailerId().equals(userService.currentRetailerId())) {
            throw new AccessDeniedException("Not your payment");
        }

        Order order = orderRepository.findByIdForUpdate(initial.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", initial.getOrderId()));
        Payment payment = paymentRepository.findByUuidForUpdate(paymentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentUuid));
        if (payment.getStatus() == Payment.PaymentTxnStatus.SUCCESS) {
            if (order.getPaymentStatus() != Enums.PaymentStatus.PAID
                    || order.getAmountPaid() == null
                    || order.getAmountPaid().compareTo(order.getTotalAmount()) < 0) {
                order.setAmountPaid(order.getTotalAmount());
                order.setLastPaymentAt(order.getLastPaymentAt() != null
                        ? order.getLastPaymentAt() : java.time.Instant.now());
                order.setPaymentStatus(Enums.PaymentStatus.PAID);
                orderRepository.save(order);
            }
            return PaymentResponse.from(payment);
        }
        if (order.getPaymentStatus() == Enums.PaymentStatus.PAID) {
            throw new BusinessException("PAYMENT_ALREADY_PAID", HttpStatus.CONFLICT,
                    "This order is already fully paid");
        }

        java.math.BigDecimal paid = order.getAmountPaid() == null
                ? java.math.BigDecimal.ZERO : order.getAmountPaid();
        java.math.BigDecimal outstanding = order.getTotalAmount().subtract(paid);
        if (outstanding.signum() <= 0) {
            throw new BusinessException("PAYMENT_ALREADY_PAID", HttpStatus.CONFLICT,
                    "This order is already fully paid");
        }
        payment.setAmount(outstanding);

        switch (payment.getMode()) {
            case WALLET -> walletService.debit(payment.getRetailerId(), payment.getAmount(),
                    payment.getId(), "Order " + order.getOrderNumber());
            case CREDIT -> creditService.charge(payment.getRetailerId(), order.getSupplierId(),
                    payment.getAmount(), payment.getId());
            case COD -> throw new BusinessException("COD_NOT_CONFIRMABLE", HttpStatus.CONFLICT,
                    "COD is settled on delivery, not via online confirmation");
            default -> throw new BusinessException("GATEWAY_VERIFICATION_REQUIRED", HttpStatus.CONFLICT,
                    "Online payments must be confirmed by a verified payment-provider callback");
        }

        payment.setStatus(Payment.PaymentTxnStatus.SUCCESS);
        payment.setGatewayRef(payment.getMode().name() + "-" + payment.getUuid());
        paymentRepository.save(payment);

        order.setAmountPaid(order.getTotalAmount());
        order.setLastPaymentAt(java.time.Instant.now());
        order.setPaymentStatus(Enums.PaymentStatus.PAID);
        orderRepository.save(order);

        return PaymentResponse.from(payment);
    }

    private String gatewayFor(Enums.PaymentMode mode) {
        return switch (mode) {
            case UPI, CARD, NET_BANKING -> "RAZORPAY";
            case WALLET -> "WALLET";
            case CREDIT -> "CREDIT";
            case COD -> "COD";
            case ADVANCE -> "ADVANCE";
        };
    }
}
