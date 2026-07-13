package com.vyaparsetu.payment.service;

import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.CreditLimitExceededException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.entity.OrderStatus;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.payment.entity.CreditAccount;
import com.vyaparsetu.payment.entity.Transaction;
import com.vyaparsetu.payment.repository.CreditAccountRepository;
import com.vyaparsetu.payment.repository.TransactionRepository;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.repository.RetailerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

@Service
public class CreditService {

    /** Orders in these states never count toward credit exposure. */
    private static final Set<OrderStatus> NON_EXPOSURE =
            EnumSet.of(OrderStatus.DRAFT, OrderStatus.CANCELLED, OrderStatus.REJECTED);

    private final CreditAccountRepository creditAccountRepository;
    private final TransactionRepository transactionRepository;
    private final RetailerRepository retailerRepository;
    private final OrderRepository orderRepository;

    public CreditService(CreditAccountRepository creditAccountRepository,
                         TransactionRepository transactionRepository,
                         RetailerRepository retailerRepository,
                         OrderRepository orderRepository) {
        this.creditAccountRepository = creditAccountRepository;
        this.transactionRepository = transactionRepository;
        this.retailerRepository = retailerRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public CreditAccount setCreditLimit(Long retailerId, Long supplierId, BigDecimal limit, Long approvedBy) {
        CreditAccount account = creditAccountRepository
                .findByRetailerIdAndSupplierId(retailerId, supplierId)
                .orElseGet(() -> {
                    CreditAccount a = new CreditAccount();
                    a.setRetailerId(retailerId);
                    a.setSupplierId(supplierId);
                    return a;
                });
        account.setCreditLimit(limit);
        account.setApprovedBy(approvedBy);
        account.setStatus(CreditAccount.Status.ACTIVE);
        return creditAccountRepository.save(account);
    }

    @Transactional
    public void approveRetailer(Long retailerId, boolean approved) {
        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));
        retailer.setCreditApproved(approved);
        retailerRepository.save(retailer);
    }

    @Transactional
    public void setStatus(Long retailerId, Long supplierId, CreditAccount.Status status) {
        CreditAccount account = creditAccountRepository
                .findByRetailerIdAndSupplierId(retailerId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account", retailerId + "/" + supplierId));
        account.setStatus(status);
        creditAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public CreditAccount get(Long retailerId, Long supplierId) {
        return creditAccountRepository.findByRetailerIdAndSupplierId(retailerId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account", retailerId + "/" + supplierId));
    }

    /** Result of an order-time credit evaluation (does not mutate state). */
    public record CreditCheck(boolean hasAccount, boolean withinLimit,
                              BigDecimal available, BigDecimal limit, BigDecimal outstanding) {
    }

    /**
     * Real-time credit exposure: the retailer's total unpaid dues with this supplier,
     * summed across all live (non-cancelled/rejected/draft) orders. Computing this
     * from actual order dues means recording a payment frees credit automatically.
     */
    @Transactional(readOnly = true)
    public BigDecimal exposure(Long retailerId, Long supplierId) {
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orderRepository.findByRetailerIdAndSupplierIdOrderByPlacedAtAsc(retailerId, supplierId)) {
            if (o.getStatus() == null || NON_EXPOSURE.contains(o.getStatus())) continue;
            BigDecimal amount = o.getTotalAmount();
            if (amount == null) continue; // order still being built
            BigDecimal paid = o.getAmountPaid() == null ? BigDecimal.ZERO : o.getAmountPaid();
            BigDecimal due = amount.subtract(paid);
            if (due.signum() > 0) total = total.add(due);
        }
        return total;
    }

    /**
     * Evaluates whether an order of {@code amount} fits inside the retailer's available credit.
     * Non-throwing: callers decide how to react based on distributor policy.
     */
    @Transactional(readOnly = true)
    public CreditCheck evaluate(Long retailerId, Long supplierId, BigDecimal amount) {
        return creditAccountRepository.findByRetailerIdAndSupplierId(retailerId, supplierId)
                .map(account -> {
                    BigDecimal outstanding = exposure(retailerId, supplierId);
                    BigDecimal available = account.getCreditLimit().subtract(outstanding);
                    boolean within = amount.compareTo(available) <= 0;
                    return new CreditCheck(true, within, available, account.getCreditLimit(), outstanding);
                })
                .orElse(new CreditCheck(false, true, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    /**
     * Charge an amount to a retailer's credit line with a supplier.
     * Enforces credit approval and limit (Property 5).
     */
    @Transactional
    public void charge(Long retailerId, Long supplierId, BigDecimal amount, Long paymentId) {
        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));
        if (!retailer.isCreditApproved()) {
            throw new BusinessException("CREDIT_NOT_APPROVED", HttpStatus.FORBIDDEN,
                    "Retailer is not approved for credit");
        }
        CreditAccount account = creditAccountRepository
                .findByRetailerIdAndSupplierId(retailerId, supplierId)
                .orElseThrow(() -> new BusinessException("NO_CREDIT_ACCOUNT", HttpStatus.CONFLICT,
                        "No credit account with this supplier"));
        if (account.getStatus() != CreditAccount.Status.ACTIVE) {
            throw new BusinessException("CREDIT_SUSPENDED", HttpStatus.CONFLICT, "Credit account suspended");
        }
        BigDecimal newOutstanding = account.getOutstanding().add(amount);
        if (newOutstanding.compareTo(account.getCreditLimit()) > 0) {
            throw new CreditLimitExceededException("Credit limit exceeded. Limit: "
                    + account.getCreditLimit() + ", outstanding would be: " + newOutstanding);
        }
        account.setOutstanding(newOutstanding);
        creditAccountRepository.save(account);

        Transaction t = new Transaction();
        t.setAccountType(Transaction.AccountType.CREDIT);
        t.setAccountId(account.getId());
        t.setPaymentId(paymentId);
        t.setDirection(Transaction.Direction.DEBIT);
        t.setAmount(amount);
        t.setBalanceAfter(account.getOutstanding());
        t.setDescription("Credit charge");
        transactionRepository.save(t);
    }
}
