package org.example.ui;

import org.example.model.Account;
import org.example.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Màn hình đăng nhập — hiển thị trước khi vào MainWindow.
 *
 *  ┌──────────────────────────────────────┐
 *  │          🏗  VLXD LOGIN              │
 *  │                                      │
 *  │   Tên đăng nhập: [______________]   │
 *  │   Mật khẩu:      [______________]   │
 *  │                                      │
 *  │           [ ĐĂNG NHẬP ]             │
 *  │                                      │
 *  │   Tài khoản demo:                    │
 *  │   admin / cashier1 / warehouse1      │
 *  │   Mật khẩu: Admin@123               │
 *  └──────────────────────────────────────┘
 */
public class LoginDialog extends JDialog {

    private final AuthService authService = new AuthService();

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JLabel         lblError;
    private JButton        btnLogin;

    private Account loggedInAccount = null;  // null = chưa login hoặc cancel

    public LoginDialog(Frame parent) {
        super(parent, "Đăng nhập — Quản lý VLXD", true);
        setSize(460, 420);
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        buildUI();
        setupKeyBindings();
    }

    /** Trả về Account đã đăng nhập, null nếu đóng/cancel */
    public Account getLoggedInAccount() {
        return loggedInAccount;
    }

    // ── BUILD UI ──────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(45, 40, 35));
        setContentPane(root);

        // ── Header ──
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(new Color(60, 50, 40));
        header.setBorder(new EmptyBorder(24, 0, 16, 0));

        JLabel iconLbl = new JLabel("🏗", SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(iconLbl);

        JLabel titleLbl = new JLabel("CỬA HÀNG VẬT LIỆU XÂY DỰNG");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(new Color(210, 180, 140));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(Box.createVerticalStrut(4));
        header.add(titleLbl);

        JLabel subLbl = new JLabel("Đăng nhập hệ thống quản lý");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(160, 150, 130));
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(Box.createVerticalStrut(2));
        header.add(subLbl);

        // Accent line
        JPanel accent = new JPanel();
        accent.setBackground(new Color(210, 180, 140));
        accent.setPreferredSize(new Dimension(0, 3));
        accent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        header.add(Box.createVerticalStrut(12));
        header.add(accent);

        root.add(header, BorderLayout.NORTH);

        // ── Form ──
        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setOpaque(false);
        formWrapper.setBorder(new EmptyBorder(20, 40, 10, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setForeground(new Color(200, 185, 165));
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formWrapper.add(lblUser, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtUsername = new JTextField(18);
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setBackground(new Color(60, 55, 48));
        txtUsername.setForeground(Color.WHITE);
        txtUsername.setCaretColor(Color.WHITE);
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 90, 70), 1),
            new EmptyBorder(6, 8, 6, 8)
        ));
        formWrapper.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setForeground(new Color(200, 185, 165));
        lblPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formWrapper.add(lblPass, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtPassword = new JPasswordField(18);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setBackground(new Color(60, 55, 48));
        txtPassword.setForeground(Color.WHITE);
        txtPassword.setCaretColor(Color.WHITE);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 90, 70), 1),
            new EmptyBorder(6, 8, 6, 8)
        ));
        formWrapper.add(txtPassword, gbc);

        // Error label
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        lblError = new JLabel(" ");
        lblError.setForeground(new Color(255, 100, 100));
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        formWrapper.add(lblError, gbc);

        // Login button
        gbc.gridy = 3;
        btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(new Color(180, 130, 70));
        btnLogin.setFocusPainted(false);
        btnLogin.setBorder(new EmptyBorder(10, 0, 10, 0));
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> doLogin());
        formWrapper.add(btnLogin, gbc);

        // Demo hint
        gbc.gridy = 4;
        gbc.insets = new Insets(16, 4, 4, 4);
        JLabel hintLbl = new JLabel("<html><div style='text-align:center;color:#9a9080;font-size:10px;'>"
            + "Tài khoản demo: <b>admin</b> / <b>cashier1</b> / <b>warehouse1</b> / <b>hr1</b> / <b>manager1</b>"
            + "<br>Mật khẩu chung: <b>Admin@123</b>"
            + "</div></html>");
        hintLbl.setHorizontalAlignment(SwingConstants.CENTER);
        formWrapper.add(hintLbl, gbc);

        root.add(formWrapper, BorderLayout.CENTER);
    }

    // ── KEY BINDINGS ──────────────────────────────────────────
    private void setupKeyBindings() {
        // Enter → login
        Action loginAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { doLogin(); }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "doLogin");
        getRootPane().getActionMap().put("doLogin", loginAction);

        // Escape → close app
        Action closeAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                int opt = JOptionPane.showConfirmDialog(LoginDialog.this,
                    "Thoát ứng dụng?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) System.exit(0);
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", closeAction);
    }

    // ── LOGIN LOGIC ───────────────────────────────────────────
    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");
        lblError.setText(" ");

        SwingWorker<Account, Void> worker = new SwingWorker<>() {
            private String errorMsg;

            @Override
            protected Account doInBackground() {
                try {
                    return authService.login(username, password);
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");
                try {
                    Account account = get();
                    if (account != null) {
                        loggedInAccount = account;
                        dispose(); // Close dialog, return to Main
                    } else {
                        lblError.setText("⚠ " + (errorMsg != null ? errorMsg : "Lỗi không xác định"));
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                    }
                } catch (Exception e) {
                    lblError.setText("⚠ Lỗi hệ thống");
                }
            }
        };
        worker.execute();
    }
}

