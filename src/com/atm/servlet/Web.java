package com.atm.servlet;

import com.atm.model.*;
import com.atm.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

/** Controller helpers and presentation-only mapping shared by servlets. */
public abstract class Web extends HttpServlet {
    protected static final BankingService BANK = new BankingService();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try { super.service(req, resp); }
        catch (BankingException e) {
            if (e.getCause() != null) getServletContext().log("Banking operation failed", e);
            if (e instanceof AccountLockedException) req.getSession().removeAttribute("userId");
            error(req, resp, e instanceof AuthenticationException ? 401 : 400, e.getMessage());
        } catch (RuntimeException e) {
            getServletContext().log("Unexpected application failure", e);
            error(req, resp, 500, "The operation could not be completed. Please try again later.");
        }
    }

    public static String escape(Object value) {
        return value == null ? "" : value.toString().replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    public static String csrf(HttpServletRequest req) {
        HttpSession session = req.getSession();
        String token = (String) session.getAttribute("csrf");
        if (token == null) {
            byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute("csrf", token);
        }
        return token;
    }
    public static void view(HttpServletRequest req, HttpServletResponse resp, String page) throws ServletException, IOException {
        req.setAttribute("context", req.getContextPath());
        req.setAttribute("csrf", csrf(req));
        Object flash = req.getSession().getAttribute("flash");
        req.setAttribute("message", escape(flash)); req.getSession().removeAttribute("flash");
        req.getRequestDispatcher("/WEB-INF/views/" + page + ".jsp").forward(req, resp);
    }
    public static void error(HttpServletRequest req, HttpServletResponse resp, int code, String message) throws ServletException, IOException {
        resp.setStatus(code); req.setAttribute("error", escape(message)); view(req, resp, "error");
    }
    protected static long userId(HttpServletRequest req) {
        Object id = req.getSession().getAttribute("userId");
        if (!(id instanceof Long userId)) throw new AuthenticationException("Please log in again.");
        return userId;
    }
    protected static void rotateSession(HttpServletRequest req) {
        req.getSession().invalidate(); req.getSession(true); req.changeSessionId(); csrf(req);
    }
    protected static void success(HttpServletRequest req, HttpServletResponse resp, String path, String message) throws IOException {
        req.getSession().setAttribute("flash", message);
        resp.setStatus(303); resp.setHeader("Location", req.getContextPath() + path);
    }
    protected static char[] secret(HttpServletRequest req, String name, int max) {
        String value = req.getParameter(name);
        if (value == null || value.length() > max) throw new BankingException("Invalid secret input.");
        return value.toCharArray();
    }
    protected static Map<String, String> customerView(CustomerSummary c) {
        return Map.of("name", escape(c.fullName()), "number", c.accountNumber(), "balance", c.balance().toPlainString(),
            "status", c.locked() ? "Locked" : "Active", "attempts", Integer.toString(c.failedAttempts()));
    }
    protected static List<Map<String, String>> transactionViews(List<BankTransaction> transactions) {
        return transactions.stream().map(t -> Map.of("time", escape(t.createdAt()), "type", t.type().name(),
            "amount", t.amount().toPlainString(), "counterparty", escape(t.counterpartyAccountNumber()),
            "reference", escape(t.referenceNumber()), "balance", t.balanceAfter().toPlainString(), "notes", escape(t.notes()))).toList();
    }
}
