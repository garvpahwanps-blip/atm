package com.atm.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;

@WebServlet("/register")
public final class RegistrationServlet extends Web {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { view(req, resp, "register"); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        char[] pin = secret(req, "pin", 4);
        try {
            var customer = BANK.register(req.getParameter("name"), pin);
            success(req, resp, "/login", "Account created. Your account number is " + customer.accountNumber() + ". Please log in.");
        } finally { Arrays.fill(pin, '\0'); }
    }
}
