<%@ include file="header.jspf" %>
<section class="card narrow"><p class="eyebrow">ADD FUNDS</p><h1>Deposit</h1><form method="post" action="${context}/deposit"><input type="hidden" name="csrf" value="${csrf}"><label>Amount<input type="number" name="amount" required min="0.01" max="1000000000" step="0.01" inputmode="decimal"></label><label>Notes (optional)<input name="notes" maxlength="255"></label><button type="submit">Confirm deposit</button></form></section>
<%@ include file="footer.jspf" %>
