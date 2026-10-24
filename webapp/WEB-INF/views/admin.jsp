<%@ page import="java.util.List,java.util.Map" %>
<%@ include file="header.jspf" %>
<% if (!Boolean.TRUE.equals(request.getAttribute("adminAuthenticated"))) { %>
<section class="card narrow"><p class="eyebrow">ADMINISTRATION</p><h1>Administrator login</h1><form method="post" action="${context}/admin"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="login"><label>Username<input name="username" required maxlength="30" autocomplete="username"></label><label>Password<input type="password" name="password" required maxlength="256" autocomplete="current-password"></label><button type="submit">Sign in</button></form></section>
<% } else { %>
<p class="eyebrow">ADMINISTRATION</p><h1>Customer accounts</h1><div class="actions"><form method="get" action="${context}/admin"><label>Search account number<input name="accountNumber" pattern="[0-9]{12}" maxlength="12" placeholder="All customers if empty"></label><button type="submit">Search</button></form><form method="post" action="${context}/admin"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="logout"><button class="secondary" type="submit">Admin logout</button></form></div>
<section class="card table-scroll"><table><thead><tr><th>Name</th><th>Account</th><th>Balance</th><th>Status</th><th>Actions</th></tr></thead><tbody>
<% for (Map<String,String> c : (List<Map<String,String>>) request.getAttribute("customers")) { %>
<tr><td><%= c.get("name") %></td><td><%= c.get("number") %></td><td><%= c.get("balance") %></td><td><%= c.get("status") %></td><td><a href="${context}/admin?selected=<%= c.get("number") %>">History</a><form method="post" action="${context}/admin"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="unlock"><input type="hidden" name="accountNumber" value="<%= c.get("number") %>"><button class="link" type="submit">Unlock</button></form></td></tr>
<% } %></tbody></table></section>
<% if (request.getAttribute("transactions") != null) { %><section class="card"><h2>Customer transaction history</h2><%@ include file="transactions.jspf" %></section><% } %>
<% } %>
<%@ include file="footer.jspf" %>
