package com.test.Exceptions;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class InsufficientFundsException extends ApiException {
    public InsufficientFundsException(UUID walletId, Long balance, Long amount) {
        super(String.format(
                "Insufficient funds in wallet %s. Balance: %d, required: %d",
                walletId, balance, amount
        ));
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST; // 400
    }
}