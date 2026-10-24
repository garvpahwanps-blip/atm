package com.atm.tests;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/** End-to-end requests to the actual Servlet/JSP application, not a REST API. */
public final class WebSmokeTest {
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
    private final String base = System.getenv().getOrDefault("ATM_BASE_URL", "http://127.0.0.1:8080/atm");
    private int checks;
    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(String path, Map<String,String> values) throws Exception {
        StringJoiner body = new StringJoiner("&");
        values.forEach((key, value) -> body.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
    }
    private String token(String path) throws Exception {
        var response = get(path);
        check(response.statusCode() == 200, "GET " + path + " returned " + response.statusCode());
        Matcher matcher = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(response.body());
        if (!matcher.find()) throw new AssertionError("No CSRF token on " + path);
        return matcher.group(1);
    }
    private String register(String name, String pin) throws Exception {
        check(post("/register", Map.of("csrf", token("/register"), "name", name, "pin", pin)).statusCode() == 303, "Registration");
        String body = get("/login").body();
        Matcher number = Pattern.compile("account number is ([0-9]{12})").matcher(body);
        if (!number.find()) throw new AssertionError("Missing generated account number");
        return number.group(1);
    }
    private String sessionId() {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    }
    private void run() throws Exception {
        check(get("/").statusCode() == 200, "Landing page");
        for (String route : List.of("/dashboard", "/deposit", "/withdraw", "/transfer", "/statement", "/change-pin"))
            check(get(route).statusCode() == 302, "Protect " + route);
        check(get("/WEB-INF/views/dashboard.jsp").statusCode() == 404, "Protect direct JSP access");
        check(post("/register", Map.of("name", "Forged Request", "pin", "1111")).statusCode() == 403, "Registration CSRF");
        String recipient = register("Web Recipient", "5678");
        String sender = register("Web Customer", "1234");
        String csrf = token("/login"), originalSession = sessionId();
        check(post("/login", Map.of("csrf", csrf, "accountNumber", sender, "pin", "1234")).statusCode() == 303, "Login");
        check(!originalSession.equals(sessionId()), "Rotate login session ID");
        var dashboard = get("/dashboard");
        check(dashboard.statusCode() == 200 && dashboard.body().contains(sender) && dashboard.body().contains("Web Customer"), "Dashboard data");
        check(dashboard.headers().firstValue("Cache-Control").orElse("").contains("no-store"), "No cache");
        check(dashboard.headers().firstValue("Content-Security-Policy").isPresent(), "CSP");
        check(post("/deposit", Map.of("csrf", csrf, "amount", "1")).statusCode() == 403, "Old session token rejected");
        csrf = token("/deposit");
        check(post("/deposit", Map.of("csrf", csrf, "amount", "100.25", "notes", "<script>alert(1)</script>")).statusCode() == 303, "Deposit");
        dashboard = get("/dashboard");
        check(dashboard.body().contains("100.25"), "Deposited balance");
        check(dashboard.body().contains("&lt;script&gt;") && !dashboard.body().contains("<script>"), "Escape transaction notes");
        check(post("/withdraw", Map.of("csrf", csrf, "amount", "999")).statusCode() == 400, "Reject overdraft");
        check(post("/withdraw", Map.of("csrf", csrf, "amount", "10.25")).statusCode() == 303, "Withdraw");
        check(post("/transfer", Map.of("csrf", csrf, "recipient", recipient, "amount", "20")).statusCode() == 303, "Transfer");
        check(get("/dashboard").body().contains("70.00"), "Final balance");
        var statement = get("/statement");
        check(statement.statusCode() == 200 && statement.body().contains("TRANSFER_OUT") && statement.body().contains(recipient), "Mini statement");
        check(post("/change-pin", Map.of("csrf", csrf, "currentPin", "1234", "newPin", "4321")).statusCode() == 303, "Change PIN");
        check(get("/dashboard").statusCode() == 302, "PIN change ends prior session");
        check(post("/login", Map.of("csrf", token("/login"), "accountNumber", sender, "pin", "4321")).statusCode() == 303, "New PIN login");
        check(post("/logout", Map.of("csrf", token("/dashboard"))).statusCode() == 303, "Logout");
        check(get("/dashboard").statusCode() == 302, "Logged out session blocked");
        csrf = token("/admin");
        check(post("/admin", Map.of("csrf", csrf, "action", "unlock", "accountNumber", sender)).statusCode() == 401, "Admin operation requires authentication");
        String passwordFile = System.getenv("ATM_TEST_ADMIN_PASSWORD_FILE");
        if (passwordFile != null) {
            String password = Files.readString(Path.of(passwordFile)).strip();
            check(post("/admin", Map.of("csrf", token("/admin"), "action", "login", "username", "admin", "password", password)).statusCode() == 303, "Admin login");
            check(get("/admin").body().contains(sender), "Admin customer list");
            var selected = get("/admin?selected=" + sender + "&accountNumber=" + sender);
            check(selected.statusCode() == 200 && selected.body().contains("TRANSFER_OUT"), "Admin search/history JSP");
            check(post("/admin", Map.of("csrf", token("/admin"), "action", "unlock", "accountNumber", sender)).statusCode() == 303, "Admin unlock");
            check(post("/admin", Map.of("csrf", token("/admin"), "action", "logout")).statusCode() == 303, "Admin logout");
        } else System.out.println("Authenticated admin HTTP checks skipped: ATM_TEST_ADMIN_PASSWORD_FILE not set");
        System.out.println("Servlet/JSP HTTP smoke: " + checks + " checks passed");
    }
    public static void main(String[] args) throws Exception { new WebSmokeTest().run(); }
}
