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

import java.util.UUID;

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
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", req.orderId()));
        if (!order.getRetailerId().equals(retailerId)) {
            throw new AccessDeniedException("Not your order");
        }
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setRetailerId(retailerId);
        payment.setMode(req.mode());
        payment.setAmount(order.getTotalAmount());
        payment.setGateway(gatewayFor(req.mode()));
        payment.setStatus(Payment.PaymentTxnStatus.INITIATED);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse confirm(String paymentUuid, String gatewayRef) {
        Payment payment = paymentRepository.findByUuid(paymentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentUuid));
        if (!payment.getRetailerId().equals(userService.currentRetailerId())) {
            throw new AccessDeniedException("Not your payment");
        }
        // idempotency (Property 8)
        if (payment.getStatus() == Payment.PaymentTxnStatus.SUCCESS) {
            return PaymentResponse.from(payment);
        }
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", payment.getOrderId()));

        switch (payment.getMode()) {
            case WALLET -> walletService.debit(payment.getRetailerId(), payment.getAmount(),
                    payment.getId(), "Order " + order.getOrderNumber());
            case CREDIT -> creditService.charge(payment.getRetailerId(), order.getSupplierId(),
                    payment.getAmount(), payment.getId());
            case COD -> throw new BusinessException("COD_NOT_CONFIRMABLE", HttpStatus.CONFLICT,
                    "COD is settled on delivery, not via online confirmation");
            default -> {
                // UPI/CARD/NET_BANKING/ADVANCE: gateway confirmed externally
            }
        }

        payment.setStatus(Payment.PaymentTxnStatus.SUCCESS);
        payment.setGatewayRef(gatewayRef != null ? gatewayRef : "REF-" + UUID.randomUUID());
        paymentRepository.save(payment);

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
