package com.test.wallet_service;

import com.test.DTO.WalletDTO.WalletBalanceResponse;
import com.test.DTO.WalletDTO.WalletOperationRequest;
import com.test.Models.Wallet;
import com.test.Repositories.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.test.DTO.WalletDTO.OperationType.DEPOSIT;
import static org.junit.jupiter.api.Assertions.assertAll;

@Import(TestcontainersConfiguration.class)
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletServiceLoadTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletRepository walletRepository;


    private static final int REQUESTS = 1000;
    private static final int THREADS = 50;
    private static final long INITIAL_BALANCE = 1_000_000L;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Test
    void shouldHandle1000rps() throws InterruptedException {
        // Setup
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(0L);
        wallet.setCurrency("RUB");
        walletRepository.save(wallet);

        restTemplate.postForEntity(
                "/api/v1/wallet",
                new WalletOperationRequest(walletId, DEPOSIT, INITIAL_BALANCE),
                Object.class
        );

        // Контрольные точки
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger duplicateCount = new AtomicInteger();
        Set<Long> operationIds = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch latch = new CountDownLatch(REQUESTS);

        // When
        for (int i = 0; i < REQUESTS; i++) {
            final long requestId = i; // Уникальный ID запроса
            executor.submit(() -> {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            "/api/v1/wallet",
                            new WalletOperationRequest(walletId, DEPOSIT, 1L),
                            String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK) {
                        if (operationIds.add(requestId)) {
                            successCount.incrementAndGet();
                        } else {
                            duplicateCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // Логируем ошибку, но не считаем как потерю
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(2, TimeUnit.MINUTES);
        executor.shutdown();

        // Then
        long finalBalance = restTemplate.getForEntity(
                "/api/v1/wallets/" + walletId,
                WalletBalanceResponse.class
        ).getBody().balance();

        long expectedOperations = INITIAL_BALANCE + successCount.get();

        assertAll(
                () -> assertThat(finalBalance)
                        .as("Баланс должен быть равен начальному + успешные операции")
                        .isEqualTo(expectedOperations),

                () -> assertThat(duplicateCount.get())
                        .as("Количество дублированных операций")
                        .isZero(),

                () -> assertThat(successCount.get())
                        .as("Количество успешных операций")
                        .isEqualTo(REQUESTS)
        );

        System.out.printf(
                "Итог: баланс=%d (ожидалось=%d), успешные=%d, дубли=%d%n",
                finalBalance, expectedOperations, successCount.get(), duplicateCount.get()
        );
    }
}