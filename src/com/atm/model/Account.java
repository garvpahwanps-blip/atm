package com.atm.model;

import java.math.BigDecimal;

public record Account(long id, long userId, BigDecimal balance, String accountType) {}
