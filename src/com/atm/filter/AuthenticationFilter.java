package com.atm.filter;

import com.atm.servlet.Web;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@WebFilter("/*")
public final class AuthenticationFilter implements Filter {
    private static final Set<String> PROTECTED = Set.of("/dashboard", "/deposit", "/withdraw", "/transfer", "/statement", "/change-pin", "/logout");
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8"); resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        String path = req.getServletPath();
        HttpSession session = req.getSession(false);
        if (PROTECTED.contains(path) && (session == null || !(session.getAttribute("userId") instanceof Long))) {
            resp.sendRedirect(req.getContextPath() + "/login"); return;
        }
        if ("POST".equals(req.getMethod())) {
            String expected = session == null ? null : (String) session.getAttribute("csrf");
            String submitted = req.getParameter("csrf");
            if (expected == null || submitted == null || submitted.length() > 100 ||
                !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8))) {
                Web.error(req, resp, 403, "Invalid form token. Reload the form and try again."); return;
            }
        }
        chain.doFilter(req, resp);
    }
}
