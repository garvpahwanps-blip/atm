package com.atm.service;

public class InsufficientBalanceException extends BankingException {
    public InsufficientBalanceException() { super("Insufficient balance."); }
}
