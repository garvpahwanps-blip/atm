package com.atm.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/dashboard")
public final class DashboardServlet extends Web {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = userId(req);
        req.setAttribute("customer", customerView(BANK.customer(id)));
        req.setAttribute("transactions", transactionViews(BANK.miniStatement(id)));
        view(req, resp, "dashboard");
    }
}
