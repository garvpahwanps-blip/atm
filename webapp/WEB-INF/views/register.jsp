<%@ include file="header.jspf" %>
<section class="card narrow"><p class="eyebrow">GET STARTED</p><h1>Open your account</h1><p>A unique 12-digit account number will be generated for you.</p>
<form method="post" action="${context}/register"><input type="hidden" name="csrf" value="${csrf}"><label>Full name<input name="name" required minlength="2" maxlength="100" autocomplete="name"></label><label>Four-digit PIN<input type="password" name="pin" required pattern="[0-9]{4}" minlength="4" maxlength="4" inputmode="numeric" autocomplete="new-password"></label><button type="submit">Create account</button></form><a href="${context}/login">Already have an account? Log in</a></section>
<%@ include file="footer.jspf" %>
