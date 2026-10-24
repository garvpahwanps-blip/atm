package com.atm.model;

import java.math.BigDecimal;

/** Safe presentation model without authentication material. */
public record CustomerSummary(long userId, long accountId, String fullName,
                              String accountNumber, BigDecimal balance,
                              boolean locked, int failedAttempts) {}
