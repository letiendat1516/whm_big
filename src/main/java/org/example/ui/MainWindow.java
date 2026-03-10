package org.example.ui;

import org.example.model.Account;
import org.example.model.UserSession;
import org.example.ui.crm.CRMPanel;
import org.example.ui.hr.HRPanel;
import org.example.ui.inventory.InventoryPanel;
import org.example.ui.pos.POSPanel;
import org.example.ui.pos.PaymentPanel;
import org.example.ui.product.ProductPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MainWindow — Cửa Hàng Vật Liệu Xây Dựng
 * Construction Materials Store — Main Navigation Shell.
 *
 * Layout:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  TOP HEADER — store name / logo / clock / sync status   │
 *  ├──────────┬──────────────────────────────────────────────┤
 *  │ SIDEBAR  │  MODULE PANEL (CardLayout)                   │
 *  │ nav btns │                                              │
 *  ├──────────┴──────────────────────────────────────────────┤
 *  │  STATUS BAR                                             │
 *  └─────────────────────────────────────────────────────────┘
 */
public class MainWindow extends JFrame {

    private static final String STORE_CODE = "VLXD-001";
    private static final String STORE_NAME = "Cửa Hàng Vật Liệu Xây Dựng";

    private final CardLayout cardLayout   = new CardLayout();
    private final JPanel     centerPanel  = new JPanel(cardLayout);
    private       JButton    activeBtn    = null;

    // Module panels (lazy-loaded)
    private POSPanel       posPanel;
    private PaymentPanel   paymentPanel;
    private InventoryPanel inventoryPanel;
    private ProductPanel   productPanel;
    private CRMPanel       crmPanel;
    private HRPanel        hrPanel;

    private final JLabel lblClock = new JLabel();
    private final JLabel lblSync  = new JLabel("● LOCAL");
    private final JLabel lblUser  = new JLabel();

    private final Account currentAccount;

    public MainWindow(Account account) {
        this.currentAccount = account;
        setTitle("🏗  " + STORE_NAME + "  [" + STORE_CODE + "]  —  " + account.getFullName() + " (" + account.getRoleName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 820);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        buildUI();
        startClock();
        showFirstAllowedModule();
    }

    // ─────────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.COLOR_BG);

        add(buildTopHeader(),  BorderLayout.NORTH);
        add(buildSidebar(),    BorderLayout.WEST);

        centerPanel.setBackground(UIUtils.COLOR_BG);
        add(centerPanel, BorderLayout.CENTER);

        add(buildStatusBar(),  BorderLayout.SOUTH);
    }

    // ── TOP HEADER ────────────────────────────────────────────────
    private JPanel buildTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.COLOR_TOPBAR);
        header.setBorder(new EmptyBorder(8, 16, 8, 16));
        header.setPreferredSize(new Dimension(0, 56));

        // Left — Logo + Store name
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel("🏗");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        left.add(icon);

        JPanel nameBlock = new JPanel(new GridLayout(2, 1, 0, 0));
        nameBlock.setOpaque(false);
        JLabel storeName = new JLabel(STORE_NAME);
        storeName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        storeName.setForeground(Color.WHITE);
        JLabel storeCode = new JLabel("Mã cửa hàng: " + STORE_CODE);
        storeCode.setFont(UIUtils.FONT_SMALL);
        storeCode.setForeground(new Color(200, 185, 165));
        nameBlock.add(storeName);
        nameBlock.add(storeCode);
        left.add(nameBlock);

        header.add(left, BorderLayout.WEST);

        // Right — user info + clock + sync + logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        // User info
        lblUser.setText("👤 " + currentAccount.getFullName());
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(210, 180, 140));
        right.add(lblUser);

        JLabel roleLbl = new JLabel("[" + currentAccount.getRoleName() + "]");
        roleLbl.setFont(UIUtils.FONT_SMALL);
        roleLbl.setForeground(new Color(160, 150, 130));
        right.add(roleLbl);

        // Separator
        JSeparator sep1 = new JSeparator(JSeparator.VERTICAL);
        sep1.setForeground(new Color(100, 90, 80));
        sep1.setPreferredSize(new Dimension(1, 24));
        right.add(sep1);

        lblSync.setFont(UIUtils.FONT_BOLD);
        lblSync.setForeground(new Color(130, 210, 130));
        right.add(lblSync);

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(new Color(100, 90, 80));
        sep.setPreferredSize(new Dimension(1, 24));
        right.add(sep);

        lblClock.setFont(new Font("Consolas", Font.BOLD, 15));
        lblClock.setForeground(Color.WHITE);
        right.add(lblClock);

        // Separator before logout
        JSeparator sep2 = new JSeparator(JSeparator.VERTICAL);
        sep2.setForeground(new Color(100, 90, 80));
        sep2.setPreferredSize(new Dimension(1, 24));
        right.add(sep2);

        // Logout button
        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(new Color(180, 80, 60));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> doLogout());
        right.add(btnLogout);

        header.add(right, BorderLayout.EAST);

        // Bottom accent line — brick orange
        JPanel accent = new JPanel();
        accent.setBackground(UIUtils.COLOR_PRIMARY);
        accent.setPreferredSize(new Dimension(0, 3));
        header.add(accent, BorderLayout.SOUTH);

        return header;
    }

    // ── SIDEBAR ────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIUtils.COLOR_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(175, 0));

        sidebar.add(Box.createVerticalStrut(12));

        // Navigation modules — only show if user has permission
        if (canAccess("POS"))       sidebar.add(makeSidebarBtn("🛒",  "BÁN HÀNG",    "POS"));
        if (canAccess("PAYMENT"))   sidebar.add(makeSidebarBtn("💳",  "THANH TOÁN",  "PAYMENT"));
        if (canAccess("INVENTORY")) sidebar.add(makeSidebarBtn("📦",  "KHO HÀNG",    "INVENTORY"));
        if (canAccess("PRODUCT"))   sidebar.add(makeSidebarBtn("🧱",  "SẢN PHẨM",    "PRODUCT"));
        if (canAccess("CRM"))       sidebar.add(makeSidebarBtn("👥",  "KHÁCH HÀNG",  "CRM"));
        if (canAccess("HR"))        sidebar.add(makeSidebarBtn("👔",  "NHÂN SỰ",     "HR"));

        sidebar.add(Box.createVerticalGlue());

        // Divider
        JSeparator div = new JSeparator();
        div.setForeground(new Color(70, 60, 50));
        div.setMaximumSize(new Dimension(175, 1));
        sidebar.add(div);
        sidebar.add(Box.createVerticalStrut(4));

        // Settings button
        JButton btnSettings = makeSidebarBtn("⚙", "CÀI ĐẶT", null);
        sidebar.add(btnSettings);
        sidebar.add(Box.createVerticalStrut(8));

        return sidebar;
    }

    private JButton makeSidebarBtn(String icon, String label, String moduleKey) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(6, 2));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconLbl.setForeground(Color.WHITE);
        iconLbl.setOpaque(false);

        JLabel textLbl = new JLabel(label, SwingConstants.CENTER);
        textLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        textLbl.setForeground(new Color(200, 185, 165));
        textLbl.setOpaque(false);

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 2));
        content.setOpaque(false);
        content.add(iconLbl);
        content.add(textLbl);
        btn.add(content, BorderLayout.CENTER);

        btn.setBackground(UIUtils.COLOR_SIDEBAR);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(175, 70));
        btn.setPreferredSize(new Dimension(175, 70));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn != activeBtn) btn.setBackground(new Color(60, 50, 40));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn != activeBtn) btn.setBackground(UIUtils.COLOR_SIDEBAR);
            }
        });

        if (moduleKey != null) {
            btn.addActionListener(e -> {
                setActiveBtn(btn);
                showModule(moduleKey);
            });
        }
        return btn;
    }

    private void setActiveBtn(JButton btn) {
        if (activeBtn != null) activeBtn.setBackground(UIUtils.COLOR_SIDEBAR);
        activeBtn = btn;
        if (activeBtn != null) activeBtn.setBackground(UIUtils.COLOR_PRIMARY);
    }

    // ── STATUS BAR ────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIUtils.COLOR_TOPBAR);
        bar.setBorder(new EmptyBorder(4, 16, 4, 16));
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.COLOR_PRIMARY),
            new EmptyBorder(4, 16, 4, 16)
        ));

        JLabel left = new JLabel("🏗  " + STORE_NAME + "  |  " + STORE_CODE
                + "  |  Phần mềm Quản lý VLXD v1.0");
        left.setForeground(new Color(200, 185, 165));
        left.setFont(UIUtils.FONT_SMALL);
        bar.add(left, BorderLayout.WEST);

        JLabel right = new JLabel("SQLite · Offline-First · WAL Mode");
        right.setForeground(new Color(150, 140, 120));
        right.setFont(UIUtils.FONT_SMALL);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ── MODULE SWITCH ─────────────────────────────────────────────
    private void showModule(String key) {
        switch (key) {
            case "POS" -> {
                if (posPanel == null) { posPanel = new POSPanel(); centerPanel.add(posPanel, "POS"); }
                cardLayout.show(centerPanel, "POS");
            }
            case "PAYMENT" -> {
                if (paymentPanel == null) { paymentPanel = new PaymentPanel(); centerPanel.add(paymentPanel, "PAYMENT"); }
                cardLayout.show(centerPanel, "PAYMENT");
            }
            case "INVENTORY" -> {
                if (inventoryPanel == null) { inventoryPanel = new InventoryPanel(); centerPanel.add(inventoryPanel, "INVENTORY"); }
                cardLayout.show(centerPanel, "INVENTORY");
            }
            case "PRODUCT" -> {
                if (productPanel == null) { productPanel = new ProductPanel(); centerPanel.add(productPanel, "PRODUCT"); }
                cardLayout.show(centerPanel, "PRODUCT");
            }
            case "CRM" -> {
                if (crmPanel == null) { crmPanel = new CRMPanel(); centerPanel.add(crmPanel, "CRM"); }
                cardLayout.show(centerPanel, "CRM");
            }
            case "HR" -> {
                if (hrPanel == null) { hrPanel = new HRPanel(); centerPanel.add(hrPanel, "HR"); }
                cardLayout.show(centerPanel, "HR");
            }
        }
    }

    // ── CLOCK ─────────────────────────────────────────────────────
    private void startClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
        Timer timer = new Timer(1000, e -> lblClock.setText(LocalDateTime.now().format(fmt)));
        timer.start();
    }

    // ── RBAC ──────────────────────────────────────────────────────

    /** Kiểm tra user hiện tại có quyền truy cập module */
    private boolean canAccess(String module) {
        if (currentAccount.isAdmin()) return true;
        return currentAccount.hasPermission(module);
    }

    /** Tự động chuyển đến module đầu tiên mà user được phép truy cập */
    private void showFirstAllowedModule() {
        String[] modules = {"POS", "PAYMENT", "INVENTORY", "PRODUCT", "CRM", "HR"};
        for (String m : modules) {
            if (canAccess(m)) {
                showModule(m);
                return;
            }
        }
    }

    /** Đăng xuất → quay lại LoginDialog */
    private void doLogout() {
        int opt = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn đăng xuất?", "Đăng xuất",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        UserSession.current().logout();
        dispose();  // Close MainWindow

        // Show login dialog again
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);

            Account account = loginDialog.getLoggedInAccount();
            if (account == null) {
                System.exit(0);
            }

            MainWindow newWindow = new MainWindow(account);
            newWindow.setVisible(true);
        });
    }
}

