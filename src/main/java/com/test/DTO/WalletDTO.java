package com.test.DTO;

import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class WalletDTO {
    // Запрос на операцию
    public record WalletOperationRequest(
            UUID walletId,
            OperationType operationType,
            @Positive Long amount
    ) {}

    public enum OperationType {
        DEPOSIT, WITHDRAW
    }

    // Ответ с балансом
    public record WalletBalanceResponse(
            UUID walletId,
            Long balance,
            String currency
    ) {}

    // Ответ на операцию
    public record WalletOperationResponse(
            UUID walletId,
            Long newBalance,
            String currency
    ) {}

    // Формат ошибки
    public record ApiError(
            String error,
            String message
    ) {}
}
