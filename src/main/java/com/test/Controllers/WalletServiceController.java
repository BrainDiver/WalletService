package com.test.Controllers;

import com.test.DTO.WalletDTO.*;
import com.test.Services.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WalletServiceController {
    private final WalletService walletService;

    public WalletServiceController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/wallet")
    public ResponseEntity<WalletOperationResponse> executeOperation(
            @RequestBody @Valid WalletOperationRequest request
    )  {
        return ResponseEntity.ok(walletService.processOperation(request));
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }
}
