<%@ include file="header.jspf" %>
<p class="eyebrow">YOUR ACCOUNT AT A GLANCE</p><h1>Welcome, ${customer.name}</h1><div class="account-grid"><section class="balance-card"><p>Available balance</p><strong>${customer.balance}</strong><p>Account ${customer.number}</p></section><section class="card"><h2>Make your next move</h2><div class="actions"><a class="button" href="${context}/deposit">Deposit</a><a class="button secondary" href="${context}/withdraw">Withdraw</a><a class="button secondary" href="${context}/transfer">Transfer funds</a></div></section></div>
<section class="card"><h2>Recent transactions</h2><%@ include file="transactions.jspf" %></section>
<%@ include file="footer.jspf" %>
