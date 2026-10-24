<%@ include file="header.jspf" %>
<section class="card narrow"><p class="eyebrow">ACCESS YOUR FUNDS</p><h1>Withdraw</h1><p>Your available balance must cover the full withdrawal.</p><form method="post" action="${context}/withdraw"><input type="hidden" name="csrf" value="${csrf}"><label>Amount<input type="number" name="amount" required min="0.01" max="1000000000" step="0.01" inputmode="decimal"></label><label>Notes (optional)<input name="notes" maxlength="255"></label><button type="submit">Confirm withdrawal</button></form></section>
<%@ include file="footer.jspf" %>
