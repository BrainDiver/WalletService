package com.test.Exceptions;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class WalletNotFoundException extends ApiException {
    public WalletNotFoundException(UUID walletId) {
        super("Wallet not found: " + walletId);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND; // 404
    }
}
