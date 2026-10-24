<%@ include file="header.jspf" %>
<section class="card narrow"><p class="eyebrow">WELCOME BACK</p><h1>Customer login</h1><p>Three incorrect PIN attempts will lock your account.</p>
<form method="post" action="${context}/login"><input type="hidden" name="csrf" value="${csrf}"><label>Account number<input name="accountNumber" required pattern="[0-9]{12}" maxlength="12" inputmode="numeric" autocomplete="username"></label><label>PIN<input type="password" name="pin" required pattern="[0-9]{4}" maxlength="4" inputmode="numeric" autocomplete="current-password"></label><button type="submit">Log in securely</button></form><a href="${context}/register">Open a new account</a></section>
<%@ include file="footer.jspf" %>
