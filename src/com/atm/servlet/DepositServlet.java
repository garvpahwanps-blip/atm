package com.atm.servlet;

import com.atm.service.Validation;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/deposit")
public final class DepositServlet extends Web {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { view(req, resp, "deposit"); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ref = BANK.deposit(userId(req), Validation.amount(req.getParameter("amount")), req.getParameter("notes"));
        success(req, resp, "/dashboard", "Deposit successful. Reference: " + ref);
    }
}
