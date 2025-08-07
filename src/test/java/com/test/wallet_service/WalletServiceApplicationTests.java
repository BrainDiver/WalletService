package com.test.wallet_service;

import com.test.DTO.WalletDTO;
import com.test.Models.Wallet;
import com.test.Repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import static com.test.DTO.WalletDTO.OperationType.DEPOSIT;
import static com.test.DTO.WalletDTO.OperationType.WITHDRAW;

@Import(TestcontainersConfiguration.class)
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletServiceApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private WalletRepository walletRepository;

	private UUID walletId;
	private final String API_URL = "/api/v1/wallets/";
	private final String OPERATION_URL = "/api/v1/wallet";

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
			.withDatabaseName("wallet_db")
			.withUsername("admin")
			.withPassword("secret");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@BeforeEach
	void setup() {
		walletRepository.deleteAll();

		// Создаем тестовый кошелек
		walletId = UUID.randomUUID();
		Wallet wallet = new Wallet();
		wallet.setId(walletId);
		wallet.setBalance(0L);
		wallet.setCurrency("RUB");
		walletRepository.save(wallet);
		restTemplate.postForEntity(OPERATION_URL,
				new WalletDTO.WalletOperationRequest(walletId, DEPOSIT, 1000L),
				WalletDTO.WalletOperationResponse.class);
	}

	@Test
	void shouldGetWalletBalance() {
		// When
		ResponseEntity<WalletDTO.WalletBalanceResponse> response = restTemplate.getForEntity(
				API_URL + walletId, WalletDTO.WalletBalanceResponse.class);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertThat(response.getBody()).isNotNull();
		assertEquals(1000L, response.getBody().balance());
	}

	@Test
	void shouldReturnNotFoundForUnknownWallet() {
		// Given
		UUID unknownId = UUID.randomUUID();

		// When
		ResponseEntity<Object> response = restTemplate.getForEntity(
				API_URL + unknownId, Object.class);

		// Then
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertThat(response.getBody()).toString().contains("Wallet not found");
	}

	@Test
	void shouldDepositFunds() {
		// Given
		WalletDTO.WalletOperationRequest request = new WalletDTO.WalletOperationRequest(walletId, DEPOSIT, 500L);

		// When
		ResponseEntity<WalletDTO.WalletOperationResponse> response = restTemplate.postForEntity(
				OPERATION_URL, request, WalletDTO.WalletOperationResponse.class);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertThat(response.getBody()).isNotNull();
		assertEquals(1500L, response.getBody().newBalance());

		// Verify balance
		ResponseEntity<WalletDTO.WalletBalanceResponse> balanceResponse = restTemplate.getForEntity(
				API_URL + walletId, WalletDTO.WalletBalanceResponse.class);
		assertEquals(1500L, balanceResponse.getBody().balance());
	}

	@Test
	void shouldWithdrawFunds() {
		// Given
		WalletDTO.WalletOperationRequest request = new WalletDTO.WalletOperationRequest(walletId, WITHDRAW, 500L);

		// When
		ResponseEntity<WalletDTO.WalletOperationResponse> response = restTemplate.postForEntity(
				OPERATION_URL, request, WalletDTO.WalletOperationResponse.class);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(500L, response.getBody().newBalance());
	}

	@Test
	void shouldReturnBadRequestForInsufficientFunds() {
		// Given
		WalletDTO.WalletOperationRequest request = new WalletDTO.WalletOperationRequest(walletId, WITHDRAW, 1500L);

		// When
		ResponseEntity<Object> response = restTemplate.postForEntity(
				OPERATION_URL, request, Object.class);

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertThat(response.getBody()).toString().contains("Insufficient funds");
	}

	@Test
	void shouldReturnBadRequestForInvalidJson() {
		// Given
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String invalidJson = "{ \"walletId\": \"invalid-uuid\", \"operationType\": \"INVALID\", \"amount\": -100 }";

		// When
		ResponseEntity<Object> response = restTemplate.exchange(
				OPERATION_URL,
				HttpMethod.POST,
				new HttpEntity<>(invalidJson, headers),
				Object.class);

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertThat(response.getBody()).toString().contains("Validation failed");
	}

	@Test
	void shouldHandleConcurrentRequests() throws InterruptedException {
		// Given
		int threads = 100;
		long amountPerThread = 10L;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch latch = new CountDownLatch(threads);

		// When
		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try {
					restTemplate.postForEntity(OPERATION_URL,
							new WalletDTO.WalletOperationRequest(walletId, DEPOSIT, amountPerThread),
							WalletDTO.WalletOperationResponse.class);
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await();

		// Then
		ResponseEntity<WalletDTO.WalletBalanceResponse> response = restTemplate.getForEntity(
				API_URL + walletId, WalletDTO.WalletBalanceResponse.class);
		assertEquals(1000L + (threads * amountPerThread), response.getBody().balance());
	}

	@Test
	void shouldHandleConcurrentWithdrawals() throws InterruptedException {
		// Setup: добавляем больше средств
		restTemplate.postForEntity(OPERATION_URL,
				new WalletDTO.WalletOperationRequest(walletId, DEPOSIT, 10000L),
				WalletDTO.WalletOperationResponse.class);

		int threads = 100;
		long amountPerThread = 100L;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch latch = new CountDownLatch(threads);

		// When
		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try {
					restTemplate.postForEntity(OPERATION_URL,
							new WalletDTO.WalletOperationRequest(walletId, WITHDRAW, amountPerThread),
							WalletDTO.WalletOperationResponse.class);
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await();

		// Then
		ResponseEntity<WalletDTO.WalletBalanceResponse> response = restTemplate.getForEntity(
				API_URL + walletId, WalletDTO.WalletBalanceResponse.class);
		long expectedBalance = 1000L + 10000L - (threads * amountPerThread);
		assertEquals(expectedBalance, response.getBody().balance());
	}
}
