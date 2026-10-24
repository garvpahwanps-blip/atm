package com.atm.model;

import java.time.Instant;

/** Internal persistence model; never store this object in a web session. */
public record User(long id, String fullName, String accountNumber, String pinHash,
                   int failedAttempts, boolean locked, Instant createdAt) {}
