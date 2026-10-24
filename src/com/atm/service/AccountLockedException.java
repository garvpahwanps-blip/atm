package com.atm.service;

public class AccountLockedException extends AuthenticationException {
    public AccountLockedException() { super("Account locked. Contact an administrator to unlock it."); }
}
