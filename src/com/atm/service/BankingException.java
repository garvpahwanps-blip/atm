package com.atm.service;

public class BankingException extends RuntimeException {
    public BankingException(String message) { super(message); }
    public BankingException(String message, Throwable cause) { super(message, cause); }
}
