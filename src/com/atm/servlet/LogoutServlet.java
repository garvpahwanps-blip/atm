package com.atm.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/logout")
public final class LogoutServlet extends Web {
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.getSession().invalidate(); resp.setStatus(303); resp.setHeader("Location", req.getContextPath() + "/login");
    }
}
