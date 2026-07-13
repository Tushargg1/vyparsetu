package com.vyaparsetu.payment.service;

import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.payment.entity.Transaction;
import com.vyaparsetu.payment.entity.Wallet;
import com.vyaparsetu.payment.repository.TransactionRepository;
import com.vyaparsetu.payment.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Wallet getOrCreate(Long retailerId) {
        return walletRepository.findByRetailerId(retailerId)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setRetailerId(retailerId);
                    w.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });
    }

    @Transactional
    public Wallet credit(Long retailerId, BigDecimal amount, Long paymentId, String description) {
        Wallet w = getOrCreate(retailerId);
        w.setBalance(w.getBalance().add(amount));
        walletRepository.save(w);
        record(w.getId(), paymentId, Transaction.Direction.CREDIT, amount, w.getBalance(), description);
        return w;
    }

    @Transactional
    public Wallet debit(Long retailerId, BigDecimal amount, Long paymentId, String description) {
        Wallet w = getOrCreate(retailerId);
        if (w.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("INSUFFICIENT_WALLET_BALANCE",
                    org.springframework.http.HttpStatus.CONFLICT, "Insufficient wallet balance");
        }
        w.setBalance(w.getBalance().subtract(amount));
        walletRepository.save(w);
        record(w.getId(), paymentId, Transaction.Direction.DEBIT, amount, w.getBalance(), description);
        return w;
    }

    private void record(Long walletId, Long paymentId, Transaction.Direction dir,
                        BigDecimal amount, BigDecimal balanceAfter, String description) {
        Transaction t = new Transaction();
        t.setAccountType(Transaction.AccountType.WALLET);
        t.setAccountId(walletId);
        t.setPaymentId(paymentId);
        t.setDirection(dir);
        t.setAmount(amount);
        t.setBalanceAfter(balanceAfter);
        t.setDescription(description);
        transactionRepository.save(t);
    }
}
