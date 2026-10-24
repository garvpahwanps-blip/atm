package com.atm.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;

@WebServlet("/change-pin")
public final class ChangePinServlet extends Web {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { view(req, resp, "change-pin"); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        char[] current = secret(req, "currentPin", 4);
        char[] next = null;
        try {
            next = secret(req, "newPin", 4);
            BANK.changePin(userId(req), current, next);
            rotateSession(req);
            success(req, resp, "/login", "PIN changed. Please log in with your new PIN.");
        } finally { Arrays.fill(current, '\0'); if (next != null) Arrays.fill(next, '\0'); }
    }
}
