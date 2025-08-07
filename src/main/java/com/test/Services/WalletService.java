package com.test.Services;

import com.test.DTO.WalletDTO.*;
import com.test.Exceptions.InsufficientFundsException;
import com.test.Exceptions.WalletNotFoundException;
import com.test.Models.Wallet;
import com.test.Repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class WalletService {
    private final WalletRepository walletRepository;

    @Autowired
    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 15, // Увеличено количество попыток
            backoff = @Backoff(delay = 50, multiplier = 2) // Увеличена задержка
    )
    public WalletOperationResponse processOperation(WalletOperationRequest request) {
        Wallet wallet = walletRepository.findById(request.walletId())
                .orElseThrow(() -> new WalletNotFoundException(request.walletId()));
        switch (request.operationType()) {
            case DEPOSIT -> deposit(wallet, request.amount());
            case WITHDRAW -> withdraw(wallet, request.amount());
        }
        wallet = walletRepository.save(wallet);
        return new WalletOperationResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrency()
        );
    }
    public void deposit(Wallet wallet, long amount) {
        wallet.setBalance(wallet.getBalance() + amount);
    }
    public void withdraw(Wallet wallet, long amount) {
        if (wallet.getBalance() < amount) {
            throw new InsufficientFundsException(
                    wallet.getId(),
                    wallet.getBalance(),
                    amount
            );
        }
        wallet.setBalance(wallet.getBalance() - amount);
    }
    public WalletBalanceResponse getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        return new WalletBalanceResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrency()
        );
    }
}