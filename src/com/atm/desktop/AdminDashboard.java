package com.atm.desktop;

import com.atm.model.*;
import com.atm.security.AdminAuthenticator;
import com.atm.service.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public final class AdminDashboard extends JFrame {
    private final BankingService bank = new BankingService();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel clock = new JLabel();
    private final JLabel status = new JLabel("Sign in to administer customer accounts.");
    private final JTextField username = new JTextField("admin", 18);
    private final JPasswordField password = new JPasswordField(18);
    private final JTextField search = new JTextField(16);
    private final DefaultTableModel customerModel = tableModel("Name", "Account number", "Balance", "Status", "Failed attempts");
    private final DefaultTableModel historyModel = tableModel("UTC timestamp", "Type", "Amount", "Counterparty", "Reference", "Balance after", "Notes");
    private final JTable customerTable = new JTable(customerModel);
    private final JTable historyTable = new JTable(historyModel);
    private List<CustomerSummary> customers = List.of();
    private final ClockThread clockThread;
    private final SessionMonitorThread sessionThread;
    private final AWTEventListener activityListener;
    private boolean authenticated;
    private boolean closed;
    private long sessionEpoch;

    public AdminDashboard() { this(Duration.ofMinutes(AdminAuthenticator.sessionMinutes())); }
    public AdminDashboard(Duration idleTimeout) {
        super("Secure ATM — Administrator");
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Construct the dashboard on the EDT");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1180, 760); setLocationRelativeTo(null); setLayout(new BorderLayout(12, 12));
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(18, 20, 10, 20));
        top.add(new JLabel("SECURE ATM  |  Administration"), BorderLayout.WEST); top.add(clock, BorderLayout.EAST);
        add(top, BorderLayout.NORTH); add(content, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(10, 20, 14, 20)); add(status, BorderLayout.SOUTH);
        content.add(loginPanel(), "login"); content.add(dashboardPanel(), "dashboard"); cards.show(content, "login");
        clockThread = new ClockThread(clock::setText);
        sessionThread = new SessionMonitorThread(idleTimeout, () -> logout("Session expired due to inactivity. Please sign in again."));
        activityListener = event -> {
            if (event.getSource() instanceof Component component &&
                SwingUtilities.getWindowAncestor(component) == this) sessionThread.touch();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
            AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { stopThreads(); } });
        clockThread.start(); sessionThread.start();
    }
    private static DefaultTableModel tableModel(String... headings) {
        return new DefaultTableModel(headings, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }
    private JPanel loginPanel() {
        JPanel outer = new JPanel(new GridBagLayout()); JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10, 10, 10, 10); g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; form.add(new JLabel("Administrator sign in"), g);
        g.gridwidth = 1; g.gridy++; form.add(new JLabel("Username"), g); g.gridx = 1; form.add(username, g);
        g.gridx = 0; g.gridy++; form.add(new JLabel("Password"), g); g.gridx = 1; form.add(password, g);
        JButton login = new JButton("Sign in"); login.addActionListener(e -> authenticate());
        password.addActionListener(e -> authenticate());
        g.gridx = 1; g.gridy++; form.add(login, g); outer.add(form); return outer;
    }
    private JPanel dashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(new JLabel("Account number:")); actions.add(search);
        JButton find = new JButton("Search"); find.addActionListener(e -> loadCustomers()); actions.add(find);
        JButton refresh = new JButton("All customers"); refresh.addActionListener(e -> { search.setText(""); loadCustomers(); }); actions.add(refresh);
        JButton history = new JButton("View history"); history.addActionListener(e -> loadHistory()); actions.add(history);
        JButton unlock = new JButton("Unlock account"); unlock.addActionListener(e -> unlockSelected()); actions.add(unlock);
        JButton logout = new JButton("Logout"); logout.addActionListener(e -> logout("Signed out.")); actions.add(logout);
        search.addActionListener(e -> loadCustomers());
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); customerTable.setAutoCreateRowSorter(true);
        // Render customer-controlled notes as plain text, never Swing HTML.
        DefaultTableCellRenderer plainText = new DefaultTableCellRenderer();
        plainText.putClientProperty("html.disable", Boolean.TRUE);
        customerTable.setDefaultRenderer(Object.class, plainText);
        historyTable.setDefaultRenderer(Object.class, plainText);
        customerTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) historyModel.setRowCount(0); });
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < historyTable.getColumnCount(); i++) historyTable.getColumnModel().getColumn(i).setPreferredWidth(i == 4 ? 290 : 160);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(customerTable), new JScrollPane(historyTable));
        split.setResizeWeight(.55); panel.add(actions, BorderLayout.NORTH); panel.add(split, BorderLayout.CENTER); return panel;
    }
    private void authenticate() {
        char[] entered = password.getPassword(); password.setText(""); String name = username.getText();
        runTask(() -> {
            try { return AdminAuthenticator.authenticate(name, entered); }
            finally { Arrays.fill(entered, '\0'); }
        }, ok -> {
            if (!ok) { status.setText("Login failed or temporarily throttled."); return; }
            authenticated = true; sessionEpoch++; sessionThread.setActive(true); cards.show(content, "dashboard");
            Toolkit.getDefaultToolkit().beep(); loadCustomers();
        });
    }
    private void loadCustomers() {
        if (!authenticated) return;
        String number = search.getText().strip();
        runTask(() -> bank.customers(number), result -> {
            customers = result; customerModel.setRowCount(0); historyModel.setRowCount(0);
            for (CustomerSummary c : result) customerModel.addRow(new Object[]{c.fullName(), c.accountNumber(), c.balance(), c.locked() ? "Locked" : "Active", c.failedAttempts()});
            status.setText(result.size() + " customer(s). Select a row to view history or unlock."); Toolkit.getDefaultToolkit().beep();
        });
    }
    private CustomerSummary selected() {
        int row = customerTable.getSelectedRow();
        if (row < 0) throw new BankingException("Select a customer first.");
        return customers.get(customerTable.convertRowIndexToModel(row));
    }
    private void loadHistory() {
        if (!authenticated) return;
        try {
            CustomerSummary customer = selected();
            runTask(() -> bank.adminHistory(customer.accountNumber()), rows -> {
                historyModel.setRowCount(0);
                for (BankTransaction t : rows) historyModel.addRow(new Object[]{t.createdAt(), t.type(), t.amount(),
                    t.counterpartyAccountNumber(), t.referenceNumber(), t.balanceAfter(), t.notes()});
                status.setText(rows.size() + " transaction(s) for " + customer.accountNumber()); Toolkit.getDefaultToolkit().beep();
            });
        } catch (BankingException e) { status.setText(e.getMessage()); }
    }
    private void unlockSelected() {
        if (!authenticated) return;
        try {
            CustomerSummary customer = selected();
            runTask(() -> { bank.unlock(customer.accountNumber()); return customer.accountNumber(); }, number -> {
                Toolkit.getDefaultToolkit().beep(); loadCustomers(); status.setText("Unlocked " + number);
            });
        } catch (BankingException e) { status.setText(e.getMessage()); }
    }
    private <T> void runTask(Callable<T> operation, Consumer<T> completed) {
        long epoch = sessionEpoch; setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return operation.call(); }
            @Override protected void done() {
                if (closed || epoch != sessionEpoch) return;
                setCursor(Cursor.getDefaultCursor());
                try { completed.accept(get()); }
                catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    if (cause instanceof BankingException && cause.getCause() == null) status.setText(cause.getMessage());
                    else { cause.printStackTrace(); status.setText("Operation failed. Check the database configuration and server log."); }
                }
            }
        }.execute();
    }
    private void logout(String message) {
        authenticated = false; sessionEpoch++; sessionThread.setActive(false);
        customers = List.of(); customerModel.setRowCount(0); historyModel.setRowCount(0); password.setText("");
        cards.show(content, "login"); setCursor(Cursor.getDefaultCursor()); status.setText(message);
    }
    private void stopThreads() {
        if (closed) return;
        closed = true; authenticated = false; sessionEpoch++;
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        clockThread.shutdown(); sessionThread.shutdown();
        try { clockThread.join(1000); sessionThread.join(1000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    @Override public void dispose() { stopThreads(); super.dispose(); }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { new AdminDashboard().setVisible(true); }
            catch (RuntimeException e) { e.printStackTrace(); JOptionPane.showMessageDialog(null, "Cannot start administrator dashboard. Check local configuration."); }
        });
    }
}
