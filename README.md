# Wallet Service API

Микросервис для управления электронными кошельками с поддержкой конкурентных операций

## Технологии
- Java 17
- Spring Boot 3
- PostgreSQL
- Liquibase (миграции БД)
- Docker

## Быстрый старт

### 1. Запуск через Docker

`git clone https://github.com/ваш-репозиторий.git`
``cd wallet-service``
```docker-compose up --build```

## 2. Примеры команд. 
В этих примерах UUID реальной записи бд которая будет создана с помощью миграций Liquibase при старте.
curl -X GET http://localhost:8080/api/v1/wallets/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{
    "walletId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "operationType": "DEPOSIT",
    "amount": 500
  }'
