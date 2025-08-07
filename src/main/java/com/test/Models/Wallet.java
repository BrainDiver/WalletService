package com.test.Models;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table
public class Wallet {
    @Id
    private UUID id;

    @Column(nullable = false)
    private Long balance = 0L;  // Храним в копейках/центах

    @Column(nullable = false, length = 3)
    private String currency = "RUB";

    @Version
    private Integer version;  // Для оптимистичной блокировки

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
