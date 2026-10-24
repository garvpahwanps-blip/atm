package com.atm.servlet;

import com.atm.security.AdminAuthenticator;
import com.atm.service.AuthenticationException;
import com.atm.service.BankingException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;

@WebServlet("/admin")
public final class AdminServlet extends Web {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean authenticated = Boolean.TRUE.equals(req.getSession().getAttribute("admin"));
        req.setAttribute("adminAuthenticated", authenticated);
        if (authenticated) {
            req.setAttribute("customers", BANK.customers(req.getParameter("accountNumber")).stream().map(Web::customerView).toList());
            String selected = req.getParameter("selected");
            if (selected != null && !selected.isBlank()) req.setAttribute("transactions", transactionViews(BANK.adminHistory(selected)));
        }
        view(req, resp, "admin");
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getParameter("action");
        if ("login".equals(action)) {
            char[] password = secret(req, "password", 256);
            try {
                if (!AdminAuthenticator.authenticate(req.getParameter("username"), password))
                    throw new AuthenticationException("Administrator login failed or temporarily throttled.");
                rotateSession(req); req.getSession().setAttribute("admin", true);
                req.getSession().setMaxInactiveInterval(AdminAuthenticator.sessionMinutes() * 60);
                success(req, resp, "/admin", "Administrator signed in.");
            } finally { Arrays.fill(password, '\0'); }
            return;
        }
        if (!Boolean.TRUE.equals(req.getSession().getAttribute("admin"))) throw new AuthenticationException("Administrator login required.");
        if ("logout".equals(action)) {
            req.getSession().invalidate(); resp.setStatus(303); resp.setHeader("Location", req.getContextPath() + "/admin");
        } else if ("unlock".equals(action)) {
            BANK.unlock(req.getParameter("accountNumber")); success(req, resp, "/admin", "Account unlocked.");
        } else throw new BankingException("Unknown administrator action.");
    }
}
