import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class ExpenseTracker {
    private Connection conn;
    private JFrame frame;
    private JTable table;
    private JTextField dateField;
    private JTextField descField;
    private JTextField amountField;
    private JTextField nameField;
    private int currentAccountId = 0;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                ExpenseTracker window = new ExpenseTracker();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ExpenseTracker() {
        initDB();
        initialize();
    }

    private void initDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = "jdbc:mysql://localhost:3306/newdatabase?useSSL=false&serverTimezone=UTC";
            String username = "root";
            String password = "root";
            conn = DriverManager.getConnection(url, username, password);

            Statement statement = conn.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (\n"
                    + " id INT AUTO_INCREMENT PRIMARY KEY,\n"
                    + " name VARCHAR(255)\n"
                    + ");\n"
                    + "CREATE TABLE IF NOT EXISTS transactions (\n"
                    + " id INT AUTO_INCREMENT PRIMARY KEY,\n"
                    + " account_id INT,\n"
                    + " type VARCHAR(10),\n"
                    + " date DATE,\n"
                    + " description VARCHAR(255),\n"
                    + " amount DOUBLE,\n"
                    + " FOREIGN KEY (account_id) REFERENCES accounts(id)\n"
                    + ");\n"
            );
            statement.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void addAccount(String accountName) {
        try {
            String sql = "INSERT INTO accounts (name) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountName);
            stmt.executeUpdate();
            stmt.close();
            JOptionPane.showMessageDialog(frame, "Account Added Successfully");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error adding account: " + e.getMessage());
        }
    }

    private void deleteAccount(int accountId) {
        try {
            String sql = "DELETE FROM accounts WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(frame, "Account deleted successfully.");
            } else {
                JOptionPane.showMessageDialog(frame, "No account found matching the ID.");
            }
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error deleting account: " + e.getMessage());
        }
    }

    public void loadData(DefaultTableModel model, int acId) {
        model.setRowCount(0);
        try {
            String sql = "SELECT type, date, description, amount FROM transactions WHERE account_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, acId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = {rs.getString("type"), rs.getString("date"), rs.getString("description"), rs.getDouble("amount")};
                model.addRow(row);
            }
            ps.close();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    private String[] getAccountDetails(int accountId) {
        String accountName = null;
        double totalExpense = 0.0;
        try {
            String accountSql = "SELECT name FROM accounts WHERE id = ?";
            PreparedStatement accountStmt = conn.prepareStatement(accountSql);
            accountStmt.setInt(1, accountId);
            ResultSet accountResult = accountStmt.executeQuery();
            if (accountResult.next()) {
                accountName = accountResult.getString("name");
                String expenseSql = "SELECT SUM(amount) AS total FROM transactions WHERE account_id = ? AND type = 'expense'";
                PreparedStatement expenseStmt = conn.prepareStatement(expenseSql);
                expenseStmt.setInt(1, accountId);
                ResultSet expenseResult = expenseStmt.executeQuery();
                if (expenseResult.next()) {
                    totalExpense = expenseResult.getDouble("total");
                }
                expenseResult.close();
                expenseStmt.close();
            }
            accountResult.close();
            accountStmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error retrieving account details: " + e.getMessage());
        }
        return new String[]{accountName, String.valueOf(totalExpense)};
    }

    private void addTransaction(int accountId, String type, String date, String description, double amount) {
        try {
            String sql = "INSERT INTO transactions (account_id, type, date, description, amount) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setString(2, type);
            stmt.setString(3, date);
            stmt.setString(4, description);
            stmt.setDouble(5, amount);
            stmt.executeUpdate();
            stmt.close();
            JOptionPane.showMessageDialog(frame, type.substring(0,1).toUpperCase() + type.substring(1) + " added successfully to Account ID: " + accountId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error adding transaction: " + e.getMessage());
        }
    }

    private void deleteTransaction(int accountId, String type, String date, String description, double amount) {
        try {
            String sql = "DELETE FROM transactions WHERE account_id = ? AND type = ? AND date = ? AND description = ? AND amount = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setString(2, type);
            stmt.setString(3, date);
            stmt.setString(4, description);
            stmt.setDouble(5, amount);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(frame, type.substring(0,1).toUpperCase() + type.substring(1) + " deleted successfully.");
            } else {
                JOptionPane.showMessageDialog(frame, "No " + type + " found matching the criteria.");
            }
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error deleting " + type + ": " + e.getMessage());
        }
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setTitle("Expense Tracker");

        JPanel topPanel = new JPanel();
        topPanel.setBounds(0, 0, 590, 58);
        frame.getContentPane().add(topPanel);
        topPanel.setLayout(null);

        JLabel lblSelectAc = new JLabel("Select A/C:");
        lblSelectAc.setBounds(0, 0, 75, 15);
        topPanel.add(lblSelectAc);

        JComboBox<String> accBox = new JComboBox<>();
        accBox.setBounds(86, 0, 130, 24);
        topPanel.add(accBox);

        JLabel lblName = new JLabel("Name:");
        lblName.setBounds(10, 37, 70, 15);
        topPanel.add(lblName);

        nameField = new JTextField();
        nameField.setBounds(86, 27, 130, 30);
        topPanel.add(nameField);
        nameField.setColumns(10);

        JButton btnAddAc = new JButton("Add A/C");
        btnAddAc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addAccount(nameField.getText());
                updateComboBox(accBox);
            }
        });
        btnAddAc.setBounds(223, 27, 117, 30);
        topPanel.add(btnAddAc);

        JButton btnDeleteAc = new JButton("Delete A/C");
        btnDeleteAc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String accountId = (String) accBox.getSelectedItem();
                accountId = accountId.substring(0, accountId.indexOf('|'));
                int id = Integer.valueOf(accountId);
                deleteAccount(id);
                updateComboBox(accBox);
            }
        });
        btnDeleteAc.setBounds(352, 27, 117, 30);
        topPanel.add(btnDeleteAc);

        JButton btnSelect = new JButton("Select");
        btnSelect.setBounds(223, 0, 117, 25);
        topPanel.add(btnSelect);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(0, 60, 590, 211);
        frame.getContentPane().add(scrollPane);

        table = new JTable();
        table.setModel(new DefaultTableModel(
                new Object[][]{{null, null, null, null}},
                new String[]{"Type", "Date", "Description", "Amount"}
        ));
        scrollPane.setViewportView(table);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBounds(0, 270, 600, 90);
        frame.getContentPane().add(bottomPanel);
        bottomPanel.setLayout(null);

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setBounds(0, 5, 50, 15);
        bottomPanel.add(dateLabel);

        dateField = new JTextField();
        dateField.setBounds(50, 5, 114, 30);
        bottomPanel.add(dateField);
        dateField.setColumns(10);

        JLabel descLabel = new JLabel("Description:");
        descLabel.setBounds(180, 5, 90, 15);
        bottomPanel.add(descLabel);

        descField = new JTextField();
        descField.setBounds(270, 5, 114, 30);
        bottomPanel.add(descField);
        descField.setColumns(10);

        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setBounds(390, 5, 70, 15);
        bottomPanel.add(amountLabel);

        amountField = new JTextField();
        amountField.setBounds(456, 5, 114, 30);
        bottomPanel.add(amountField);
        amountField.setColumns(10);

        JButton btnAddTransaction = new JButton("Add Transaction");
        btnAddTransaction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String type = JOptionPane.showInputDialog(frame, "Enter Type (expense or bill):");
                addTransaction(currentAccountId, type, dateField.getText(), descField.getText(), Double.parseDouble(amountField.getText()));
                loadData((DefaultTableModel) table.getModel(), currentAccountId);
            }
        });
        btnAddTransaction.setBounds(30, 39, 140, 25);
        bottomPanel.add(btnAddTransaction);

        JButton btnDeleteTransaction = new JButton("Delete Transaction");
        btnDeleteTransaction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    String type = (String) table.getValueAt(selectedRow, 0);
                    String date = (String) table.getValueAt(selectedRow, 1);
                    String description = (String) table.getValueAt(selectedRow, 2);
                    double amount = Double.parseDouble(table.getValueAt(selectedRow, 3).toString()); // Retrieve amount as a double
                    deleteTransaction(currentAccountId, type, date, description, amount);
                    loadData((DefaultTableModel) table.getModel(), currentAccountId);
                } else {
                    JOptionPane.showMessageDialog(frame, "Please select a transaction to delete.");
                }
            }
        });

        btnDeleteTransaction.setBounds(200, 39, 140, 25);
        bottomPanel.add(btnDeleteTransaction);

        JLabel lblTotalExpense = new JLabel("Total Expense:");
        lblTotalExpense.setBounds(20, 75, 100, 15);
        bottomPanel.add(lblTotalExpense);

        JLabel lblTotalAmount = new JLabel("--");
        lblTotalAmount.setBounds(130, 75, 70, 15);
        bottomPanel.add(lblTotalAmount);

        JLabel lblCurrAcc = new JLabel("Current Account Name:");
        lblCurrAcc.setBounds(270, 76, 150, 15);
        bottomPanel.add(lblCurrAcc);

        JLabel lblAccountName = new JLabel("Account Name");
        lblAccountName.setBounds(430, 76, 130, 15);
        bottomPanel.add(lblAccountName);

        btnSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String accountId = (String) accBox.getSelectedItem();
                accountId = accountId.substring(0, accountId.indexOf('|'));
                currentAccountId = Integer.valueOf(accountId);
                loadData((DefaultTableModel) table.getModel(), currentAccountId);
                String[] details = getAccountDetails(currentAccountId);
                lblTotalAmount.setText(details[1]);
                lblAccountName.setText(details[0]);
            }
        });

        updateComboBox(accBox);
    }

    private void updateComboBox(JComboBox<String> cbx) {
        cbx.removeAllItems();
        try {
            String sql = "SELECT * FROM accounts";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cbx.addItem(rs.getString("id") + "|" + rs.getString("name"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error updating ComboBox: " + e.getMessage());
        }
    }
}
