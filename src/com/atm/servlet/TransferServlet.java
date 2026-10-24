package com.atm.servlet;

import com.atm.service.Validation;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/transfer")
public final class TransferServlet extends Web {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { view(req, resp, "transfer"); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ref = BANK.transfer(userId(req), req.getParameter("recipient"), Validation.amount(req.getParameter("amount")), req.getParameter("notes"));
        success(req, resp, "/dashboard", "Transfer successful. Reference: " + ref);
    }
}
