package com.atm.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;

@WebServlet("/login")
public final class LoginServlet extends Web {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { view(req, resp, "login"); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        char[] pin = secret(req, "pin", 4);
        try {
            var customer = BANK.login(req.getParameter("accountNumber"), pin);
            rotateSession(req); req.getSession().setAttribute("userId", customer.userId());
            success(req, resp, "/dashboard", "Welcome, " + customer.fullName() + ".");
        } finally { Arrays.fill(pin, '\0'); }
    }
}
