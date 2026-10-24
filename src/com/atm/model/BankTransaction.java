package com.atm.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BankTransaction(long id, long accountId, TransactionType type,
                              BigDecimal amount, String counterpartyAccountNumber,
                              String referenceNumber, BigDecimal balanceAfter,
                              String notes, Instant createdAt) {}
