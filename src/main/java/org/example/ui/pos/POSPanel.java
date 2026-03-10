package org.example.ui.pos;

import org.example.model.*;
import org.example.service.CRMService;
import org.example.service.POSService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Module 1 — POS Panel (Bán hàng vật liệu xây dựng)
 * Hỗ trợ: thuế VAT, thanh toán từng phần, xuất kho, trả hàng
 */
public class POSPanel extends JPanel {

    private final POSService posService = new POSService();
    private final CRMService crmService = new CRMService();

    // Session
    private String cashierId = "CSH-001";
    private String storeId   = "STORE-001";
    private String shiftId   = null;

    // Cart table
    private final String[] cartCols = {"#", "Sản phẩm", "Barcode", "Đơn giá", "SL", "Thành tiền", "ID"};
    private final DefaultTableModel cartModel = new DefaultTableModel(cartCols, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        @Override public Class<?> getColumnClass(int c) { return c == 4 ? Integer.class : Object.class; }
    };
    private final JTable cartTable = new JTable(cartModel);

    // Summary labels
    private final JLabel lblTotal    = new JLabel("0 ₫");
    private final JLabel lblDiscount = new JLabel("0 ₫");
    private final JLabel lblTax      = new JLabel("0 ₫");
    private final JLabel lblFinal    = new JLabel("0 ₫");
    private final JLabel lblChange   = new JLabel("0 ₫");
    private final JLabel lblDebt     = new JLabel("0 ₫");
    private final JLabel lblCustomer = new JLabel("Khách lẻ");
    private final JLabel lblPoints   = new JLabel("—");
    private final JLabel lblOrderId  = new JLabel("—");
    private final JLabel lblTime     = new JLabel("");

    // Input
    private final JTextField txtBarcode  = new JTextField(14);
    private final JTextField txtCustomer = new JTextField(12);
    private final JTextField txtVoucher  = new JTextField(10);
    private final JTextField txtCash     = new JTextField(12);
    private final JTextField txtPayAmount = new JTextField(12);
    private final JComboBox<String> cboPayment = new JComboBox<>(new String[]{"CASH", "QR", "CARD"});
    private final JCheckBox chkPartial = new JCheckBox("Thanh toán từng phần");

    private Customer selectedCustomer = null;
    private double discountAmount = 0;

    public POSPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.COLOR_BG);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        // Clock
        Timer clock = new Timer(1000, e ->
            lblTime.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        clock.start();

        // Shortcuts
        bindKey(KeyEvent.VK_F12, "checkout", ev -> doCheckout());
        bindKey(KeyEvent.VK_F10, "cancel",   ev -> doCancelOrder());
        bindKey(KeyEvent.VK_F2,  "neworder", ev -> startNewOrder());
        bindKey(KeyEvent.VK_F3,  "search",   ev -> openProductSearch());

        startNewOrder();
    }

    public void setSession(String cashierId, String storeId, String shiftId) {
        this.cashierId = cashierId;
        this.storeId = storeId;
        this.shiftId = shiftId;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TOOLBAR
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIUtils.COLOR_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        JButton btnNew = styledBtn("Đơn mới (F2)", UIUtils.COLOR_WARNING, Color.WHITE);
        btnNew.addActionListener(e -> startNewOrder());
        left.add(btnNew);

        JButton btnSearch = styledBtn("Tìm SP (F3)", UIUtils.COLOR_SECONDARY, Color.WHITE);
        btnSearch.addActionListener(e -> openProductSearch());
        left.add(btnSearch);

        left.add(separator());
        left.add(label("Barcode:"));
        txtBarcode.setFont(UIUtils.FONT_MONO);
        txtBarcode.setPreferredSize(new Dimension(160, 30));
        txtBarcode.setToolTipText("Quét hoặc nhập barcode sản phẩm");
        addPlaceholder(txtBarcode, "Nhập barcode...");
        txtBarcode.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doScan();
            }
        });
        left.add(txtBarcode);
        JButton btnScan = styledBtn("Quét", UIUtils.COLOR_PRIMARY, Color.WHITE);
        btnScan.addActionListener(e -> doScan());
        left.add(btnScan);

        left.add(separator());
        left.add(label("Khách hàng:"));
        txtCustomer.setPreferredSize(new Dimension(130, 30));
        txtCustomer.setToolTipText("Nhập SĐT khách hàng để tra cứu");
        addPlaceholder(txtCustomer, "Nhập SĐT...");
        left.add(txtCustomer);
        JButton btnCus = styledBtn("Tra cứu", UIUtils.COLOR_SECONDARY, Color.WHITE);
        btnCus.addActionListener(e -> findCustomer());
        txtCustomer.addActionListener(e -> findCustomer());
        left.add(btnCus);

        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        lblTime.setFont(UIUtils.FONT_MONO);
        lblTime.setForeground(UIUtils.COLOR_TEXT_MUTED);
        right.add(lblTime);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BODY
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(8, 0));
        body.setBackground(UIUtils.COLOR_BG);
        body.setBorder(new EmptyBorder(8, 8, 8, 8));
        body.add(buildCartPanel(), BorderLayout.CENTER);
        body.add(buildSidebar(), BorderLayout.EAST);
        return body;
    }

    private JPanel buildCartPanel() {
        JPanel card = cardPanel("GIỎ HÀNG");
        styleCartTable();
        JScrollPane scroll = new JScrollPane(cartTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bottom.setOpaque(false);
        JButton btnRemove = styledBtn("Xóa dòng", UIUtils.COLOR_DANGER, Color.WHITE);
        btnRemove.addActionListener(e -> removeSelectedItem());
        bottom.add(btnRemove);
        JLabel hint = new JLabel("Nhấn đúp cột SL để sửa số lượng");
        hint.setFont(UIUtils.FONT_SMALL);
        hint.setForeground(UIUtils.COLOR_TEXT_MUTED);
        bottom.add(Box.createHorizontalStrut(12));
        bottom.add(hint);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void styleCartTable() {
        cartTable.setFont(UIUtils.FONT_LABEL);
        cartTable.setRowHeight(32);
        cartTable.setShowGrid(false);
        cartTable.setIntercellSpacing(new Dimension(0, 1));
        cartTable.setSelectionBackground(new Color(255, 240, 220));
        cartTable.setSelectionForeground(Color.BLACK);
        cartTable.setFillsViewportHeight(true);
        cartTable.setGridColor(UIUtils.COLOR_BORDER);
        cartTable.setShowHorizontalLines(true);

        cartTable.getColumnModel().getColumn(6).setMinWidth(0);
        cartTable.getColumnModel().getColumn(6).setMaxWidth(0);
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(45);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(50);
        cartTable.getColumnModel().getColumn(4).setMaxWidth(65);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(110);

        cartTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setFont(UIUtils.FONT_LABEL);
                setForeground(Color.BLACK);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (sel) setBackground(new Color(255, 240, 220));
                else     setBackground(row % 2 == 0 ? Color.WHITE : UIUtils.COLOR_TABLE_EVEN);
                return this;
            }
        });

        cartTable.getTableHeader().setFont(UIUtils.FONT_BOLD);
        cartTable.getTableHeader().setBackground(UIUtils.COLOR_CARD);
        cartTable.getTableHeader().setForeground(Color.BLACK);
        cartTable.getTableHeader().setPreferredSize(new Dimension(0, 34));
        cartTable.getTableHeader().setReorderingAllowed(false);
        cartTable.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.COLOR_BORDER));

        cartModel.addTableModelListener(e -> {
            if (e.getColumn() == 4) refreshTotals();
        });
    }

    // ── Sidebar ────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(310, 0));
        sidebar.setBackground(UIUtils.COLOR_BG);
        sidebar.add(buildOrderInfoCard());
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(buildPaymentCard());
        return sidebar;
    }

    private JPanel buildOrderInfoCard() {
        JPanel card = cardPanel("THÔNG TIN ĐƠN");
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 0, 4, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        addInfoRow(grid, g, row++, "Mã đơn:", lblOrderId);
        addInfoRow(grid, g, row++, "Khách hàng:", lblCustomer);
        addInfoRow(grid, g, row++, "Điểm tích lũy:", lblPoints);
        lblOrderId.setFont(UIUtils.FONT_BOLD);
        lblOrderId.setForeground(UIUtils.COLOR_SECONDARY);
        lblCustomer.setFont(UIUtils.FONT_BOLD);
        lblCustomer.setForeground(Color.BLACK);
        lblPoints.setFont(UIUtils.FONT_BOLD);
        lblPoints.setForeground(UIUtils.COLOR_WARNING);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPaymentCard() {
        JPanel card = cardPanel("THANH TOÁN");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Totals with tax
        JPanel totals = new JPanel(new GridBagLayout());
        totals.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 0, 3, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        addAmountRow(totals, g, row++, "Tổng tiền hàng:", lblTotal, UIUtils.FONT_LABEL, Color.BLACK);
        addAmountRow(totals, g, row++, "Giảm giá:", lblDiscount, UIUtils.FONT_LABEL, UIUtils.COLOR_DANGER);
        addAmountRow(totals, g, row++, "Thuế VAT (10%):", lblTax, UIUtils.FONT_LABEL, UIUtils.COLOR_WARNING);

        g.gridx = 0; g.gridy = row++; g.gridwidth = 2;
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(UIUtils.COLOR_BORDER);
        totals.add(sep1, g);
        g.gridwidth = 1;

        lblFinal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblFinal.setForeground(UIUtils.COLOR_SUCCESS);
        addAmountRow(totals, g, row++, "THÀNH TIỀN:", lblFinal,
            new Font("Segoe UI", Font.BOLD, 14), UIUtils.COLOR_SUCCESS);

        content.add(totals);
        content.add(Box.createVerticalStrut(8));

        // Voucher
        JPanel voucherRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        voucherRow.setOpaque(false);
        voucherRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        voucherRow.add(label("Voucher:"));
        txtVoucher.setPreferredSize(new Dimension(100, 28));
        voucherRow.add(txtVoucher);
        JButton btnVoucher = styledBtn("Áp dụng", UIUtils.COLOR_SECONDARY, Color.WHITE);
        btnVoucher.setPreferredSize(new Dimension(80, 28));
        btnVoucher.addActionListener(e -> applyVoucher());
        voucherRow.add(btnVoucher);
        content.add(voucherRow);
        content.add(Box.createVerticalStrut(6));

        JSeparator sep2 = new JSeparator();
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        content.add(sep2);
        content.add(Box.createVerticalStrut(6));

        // Payment method
        JPanel pmRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pmRow.setOpaque(false);
        pmRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        pmRow.add(label("Phương thức:"));
        cboPayment.setFont(UIUtils.FONT_LABEL);
        cboPayment.setPreferredSize(new Dimension(100, 28));
        cboPayment.addActionListener(e -> toggleCashField());
        pmRow.add(cboPayment);
        content.add(pmRow);
        content.add(Box.createVerticalStrut(4));

        // Partial payment checkbox
        JPanel partialRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        partialRow.setOpaque(false);
        partialRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        chkPartial.setFont(UIUtils.FONT_LABEL);
        chkPartial.setOpaque(false);
        chkPartial.setForeground(Color.BLACK);
        chkPartial.addActionListener(e -> {
            txtPayAmount.setEnabled(chkPartial.isSelected());
            if (!chkPartial.isSelected()) txtPayAmount.setText("");
        });
        partialRow.add(chkPartial);
        content.add(partialRow);
        content.add(Box.createVerticalStrut(4));

        // Pay amount (for partial payment)
        JPanel payAmtRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        payAmtRow.setOpaque(false);
        payAmtRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        payAmtRow.add(label("Số tiền trả:"));
        txtPayAmount.setFont(UIUtils.FONT_MONO);
        txtPayAmount.setPreferredSize(new Dimension(130, 28));
        txtPayAmount.setEnabled(false);
        txtPayAmount.setToolTipText("Để trống = trả toàn bộ");
        payAmtRow.add(txtPayAmount);
        content.add(payAmtRow);
        content.add(Box.createVerticalStrut(4));

        // Cash received
        JPanel cashRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        cashRow.setOpaque(false);
        cashRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        cashRow.add(label("Tiền nhận:"));
        txtCash.setFont(UIUtils.FONT_MONO);
        txtCash.setPreferredSize(new Dimension(130, 28));
        txtCash.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateChange(); }
        });
        cashRow.add(txtCash);
        content.add(cashRow);
        content.add(Box.createVerticalStrut(4));

        // Change
        JPanel changeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        changeRow.setOpaque(false);
        changeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        changeRow.add(label("Tiền thừa:"));
        lblChange.setFont(UIUtils.FONT_BOLD);
        lblChange.setForeground(UIUtils.COLOR_SUCCESS);
        changeRow.add(lblChange);
        content.add(changeRow);
        content.add(Box.createVerticalStrut(4));

        // Debt display
        JPanel debtRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        debtRow.setOpaque(false);
        debtRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        debtRow.add(label("Công nợ:"));
        lblDebt.setFont(UIUtils.FONT_BOLD);
        lblDebt.setForeground(UIUtils.COLOR_DANGER);
        debtRow.add(lblDebt);
        content.add(debtRow);
        content.add(Box.createVerticalStrut(10));

        // Buttons
        JButton btnCheckout = styledBtn("THANH TOÁN   [F12]", UIUtils.COLOR_SUCCESS, Color.WHITE);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCheckout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnCheckout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCheckout.addActionListener(e -> doCheckout());
        content.add(btnCheckout);
        content.add(Box.createVerticalStrut(6));

        JButton btnCancel = styledBtn("Hủy đơn  [F10]", UIUtils.COLOR_DANGER, Color.WHITE);
        btnCancel.setFont(UIUtils.FONT_BOLD);
        btnCancel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnCancel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCancel.addActionListener(e -> doCancelOrder());
        content.add(btnCancel);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════
    private void startNewOrder() {
        cartModel.setRowCount(0);
        discountAmount = 0;
        selectedCustomer = null;
        lblCustomer.setText("Khách lẻ");
        lblPoints.setText("—");
        lblTotal.setText("0 ₫");
        lblDiscount.setText("0 ₫");
        lblTax.setText("0 ₫");
        lblFinal.setText("0 ₫");
        lblChange.setText("0 ₫");
        lblDebt.setText("0 ₫");
        txtVoucher.setText("");
        txtCash.setText("");
        txtPayAmount.setText("");
        chkPartial.setSelected(false);
        txtPayAmount.setEnabled(false);
        lblOrderId.setText("Đang tạo...");

        try {
            posService.startOrder(cashierId, storeId, shiftId);
            String oid = posService.getCurrentOrder() != null
                    ? posService.getCurrentOrder().getOrderId() : "?";
            String shortId = oid.length() > 8 ? oid.substring(0, 8).toUpperCase() : oid;
            lblOrderId.setText("#" + shortId);
            lblOrderId.setForeground(UIUtils.COLOR_SECONDARY);
        } catch (SQLException ex) {
            lblOrderId.setText("LỖI!");
            lblOrderId.setForeground(UIUtils.COLOR_DANGER);
            UIUtils.showError(this, "Không thể tạo đơn hàng:\n" + ex.getMessage()
                + "\n\nKiểm tra: cashierId='" + cashierId + "' tồn tại trong bảng Cashier?"
                + "\nstoreId='" + storeId + "' tồn tại trong bảng Store?");
        }
    }

    private void doScan() {
        String barcode = txtBarcode.getText().trim();
        if (barcode.isEmpty() || barcode.equals("Nhập barcode...")) return;
        try {
            OrderItem item = posService.scanBarcode(barcode);
            if (item == null) {
                UIUtils.showError(this, "Không tìm thấy sản phẩm với barcode: " + barcode);
                return;
            }
            refreshCartTable();
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
        txtBarcode.setText("");
        txtBarcode.requestFocus();
    }

    private void openProductSearch() {
        if (posService.getCurrentOrder() == null) {
            UIUtils.showError(this, "Chưa có đơn hàng. Hệ thống sẽ tạo mới.");
            startNewOrder();
        }
        ProductSearchDialog dlg = new ProductSearchDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this), posService);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) refreshCartTable();
    }

    private void findCustomer() {
        String phone = txtCustomer.getText().trim();
        if (phone.isEmpty() || phone.equals("Nhập SĐT...")) return;
        try {
            Customer c = crmService.lookupByPhone(phone);
            if (c == null) {
                if (UIUtils.confirm(this, "Khách hàng chưa đăng ký.\nTạo mới?")) {
                    openNewCustomerDialog(phone);
                }
                return;
            }
            selectedCustomer = c;
            posService.setCustomer(c.getCustomerId());
            lblCustomer.setText(c.getFullName()
                + (c.getTierName() != null ? " (" + c.getTierName() + ")" : ""));
            lblPoints.setText(c.getCurrentPoints() + " pts");
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void openNewCustomerDialog(String phone) {
        Customer c = new Customer();
        c.setPhoneNum(phone);
        CustomerQuickDialog dlg = new CustomerQuickDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this), c);
        dlg.setVisible(true);
        if (dlg.getResult() != null) {
            selectedCustomer = dlg.getResult();
            posService.setCustomer(selectedCustomer.getCustomerId());
            lblCustomer.setText(selectedCustomer.getFullName());
        }
    }

    private void applyVoucher() {
        String code = txtVoucher.getText().trim();
        if (code.isEmpty()) return;
        try {
            discountAmount = posService.applyVoucher(code);
            refreshTotals();
            UIUtils.showSuccess(this, "Voucher áp dụng thành công!\nGiảm: "
                + UIUtils.formatCurrency(discountAmount));
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void doCheckout() {
        if (posService.getCartItems().isEmpty()) {
            UIUtils.showError(this, "Giỏ hàng trống!");
            return;
        }
        String method = (String) cboPayment.getSelectedItem();
        double cashReceived = 0;
        double amountToPay = posService.getFinalAmount();

        // Parse partial payment amount
        if (chkPartial.isSelected()) {
            try {
                String payStr = txtPayAmount.getText().trim().replace(",", "").replace(".", "");
                if (!payStr.isEmpty()) {
                    amountToPay = Double.parseDouble(payStr);
                    if (amountToPay <= 0) {
                        UIUtils.showError(this, "Số tiền trả phải > 0.");
                        return;
                    }
                    if (amountToPay > posService.getFinalAmount()) {
                        amountToPay = posService.getFinalAmount();
                    }
                }
            } catch (NumberFormatException ex) {
                UIUtils.showError(this, "Số tiền trả không hợp lệ.");
                return;
            }
        }

        if ("CASH".equals(method)) {
            try {
                String cashStr = txtCash.getText().trim().replace(",", "").replace(".", "");
                cashReceived = cashStr.isEmpty() ? amountToPay : Double.parseDouble(cashStr);
                if (cashReceived < amountToPay) {
                    UIUtils.showError(this, "Tiền khách đưa không đủ cho số tiền cần trả!");
                    return;
                }
            } catch (NumberFormatException ex) {
                UIUtils.showError(this, "Số tiền không hợp lệ.");
                return;
            }
        }

        // Confirm partial payment
        if (chkPartial.isSelected() && amountToPay < posService.getFinalAmount()) {
            double debt = posService.getFinalAmount() - amountToPay;
            if (!UIUtils.confirm(this, String.format(
                    "Khách trả: %s\nCông nợ còn lại: %s\n\nXác nhận thanh toán từng phần?",
                    UIUtils.formatCurrency(amountToPay), UIUtils.formatCurrency(debt)))) {
                return;
            }
        }

        try {
            POSService.CheckoutResult result = posService.checkout(method, cashReceived, amountToPay);
            showReceiptDialog(result, method, cashReceived);
            startNewOrder();
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi thanh toán: " + ex.getMessage());
        }
    }

    private void doCancelOrder() {
        if (!UIUtils.confirm(this, "Hủy đơn hàng hiện tại?")) return;
        try {
            posService.cancelCurrentOrder();
            startNewOrder();
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void removeSelectedItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn dòng cần xóa."); return; }
        String itemId = (String) cartModel.getValueAt(row, 6);
        try {
            posService.removeFromCart(itemId);
            refreshCartTable();
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void toggleCashField() {
        boolean isCash = "CASH".equals(cboPayment.getSelectedItem());
        txtCash.setEnabled(isCash);
        lblChange.setText(isCash ? "0 ₫" : "—");
    }

    private void updateChange() {
        try {
            double cash = Double.parseDouble(
                txtCash.getText().replace(",", "").replace(".", "").trim());
            double payAmt = posService.getFinalAmount();
            if (chkPartial.isSelected()) {
                try {
                    String ps = txtPayAmount.getText().replace(",", "").replace(".", "").trim();
                    if (!ps.isEmpty()) payAmt = Double.parseDouble(ps);
                } catch (NumberFormatException ignored) {}
            }
            double change = Math.max(0, cash - payAmt);
            lblChange.setText(UIUtils.formatCurrency(change));
        } catch (NumberFormatException ignored) {}
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        List<OrderItem> items = posService.getCartItems();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            cartModel.addRow(new Object[]{
                i + 1,
                item.getProductName() + " — " + item.getVariantName(),
                item.getBarcode(),
                UIUtils.formatCurrency(item.getUnitPrice()),
                item.getQuantity(),
                UIUtils.formatCurrency(item.getSubtotal()),
                item.getOrderItemId()
            });
        }
        refreshTotals();
    }

    private void refreshTotals() {
        double total = posService.getCartTotal();
        double disc  = posService.getDiscountAmount();
        double tax   = posService.getTaxAmount();
        double fin   = posService.getFinalAmount();
        lblTotal.setText(UIUtils.formatCurrency(total));
        lblDiscount.setText(disc > 0 ? "-" + UIUtils.formatCurrency(disc) : "0 ₫");
        lblTax.setText(UIUtils.formatCurrency(tax));
        lblFinal.setText(UIUtils.formatCurrency(fin));

        // Update debt label if partial
        if (chkPartial.isSelected()) {
            try {
                String ps = txtPayAmount.getText().replace(",", "").replace(".", "").trim();
                if (!ps.isEmpty()) {
                    double payAmt = Double.parseDouble(ps);
                    lblDebt.setText(UIUtils.formatCurrency(Math.max(0, fin - payAmt)));
                } else {
                    lblDebt.setText("0 ₫");
                }
            } catch (NumberFormatException e) {
                lblDebt.setText("0 ₫");
            }
        } else {
            lblDebt.setText("0 ₫");
        }
        updateChange();
    }

    private void showReceiptDialog(POSService.CheckoutResult result,
                                   String method, double cashReceived) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════\n");
        sb.append("    CỬA HÀNG VẬT LIỆU XÂY DỰNG\n");
        sb.append("         HÓA ĐƠN BÁN HÀNG\n");
        sb.append("═══════════════════════════════════\n");
        sb.append(String.format("Mã đơn : %s%n",
            result.orderId().substring(0, 8).toUpperCase()));
        sb.append(String.format("Ngày   : %s%n", LocalDate.now()));
        if (selectedCustomer != null)
            sb.append(String.format("Khách  : %s%n", selectedCustomer.getFullName()));
        sb.append("───────────────────────────────────\n");

        double subtotal = result.finalAmount() - result.taxAmount() + result.discountAmount();
        sb.append(String.format("Tổng hàng: %s%n", UIUtils.formatCurrency(subtotal)));
        if (result.discountAmount() > 0)
            sb.append(String.format("Giảm giá : -%s%n", UIUtils.formatCurrency(result.discountAmount())));
        sb.append(String.format("Thuế VAT : %s%n", UIUtils.formatCurrency(result.taxAmount())));
        sb.append(String.format("T.Toán   : %s%n", UIUtils.formatCurrency(result.finalAmount())));
        sb.append("───────────────────────────────────\n");
        sb.append(String.format("PT       : %s%n", method));
        sb.append(String.format("Đã trả  : %s%n", UIUtils.formatCurrency(result.paidAmount())));
        if ("CASH".equals(method)) {
            sb.append(String.format("Nhận     : %s%n", UIUtils.formatCurrency(cashReceived)));
            sb.append(String.format("Thừa     : %s%n", UIUtils.formatCurrency(result.changeAmount())));
        }
        if (result.debtRemaining() > 0) {
            sb.append("───────────────────────────────────\n");
            sb.append(String.format("CÔNG NỢ  : %s%n", UIUtils.formatCurrency(result.debtRemaining())));
            sb.append(String.format("Trạng thái: %s%n", result.paymentStatus()));
        }
        if (result.pointsEarned() > 0)
            sb.append(String.format("Điểm+   : +%d pts%n", result.pointsEarned()));
        sb.append("═══════════════════════════════════\n");
        sb.append("        Cảm ơn quý khách!\n");

        JTextArea txt = new JTextArea(sb.toString());
        txt.setFont(UIUtils.FONT_MONO);
        txt.setEditable(false);
        txt.setBackground(UIUtils.COLOR_CARD);
        JScrollPane sp = new JScrollPane(txt);
        sp.setPreferredSize(new Dimension(400, 380));
        JOptionPane.showMessageDialog(this, sp, "Hóa đơn", JOptionPane.PLAIN_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════════
    private JPanel cardPanel(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(UIUtils.COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        if (title != null && !title.isEmpty()) {
            JLabel lbl = new JLabel(title);
            lbl.setFont(UIUtils.FONT_BOLD);
            lbl.setForeground(Color.BLACK);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER),
                new EmptyBorder(0, 0, 6, 0)
            ));
            card.add(lbl, BorderLayout.NORTH);
        }
        return card;
    }

    private JButton styledBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(UIUtils.FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int textWidth = b.getFontMetrics(b.getFont()).stringWidth(text);
        b.setPreferredSize(new Dimension(textWidth + 36, 32));
        b.setMinimumSize(new Dimension(textWidth + 36, 32));
        return b;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIUtils.FONT_LABEL);
        l.setForeground(Color.BLACK);
        return l;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(UIUtils.COLOR_BORDER);
        return sep;
    }

    private void addInfoRow(JPanel p, GridBagConstraints g, int row, String labelText, JLabel value) {
        JLabel lbl = label(labelText);
        lbl.setForeground(UIUtils.COLOR_TEXT_MUTED);
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        p.add(lbl, g);
        g.gridx = 1; g.weightx = 1;
        p.add(value, g);
    }

    private void addAmountRow(JPanel p, GridBagConstraints g, int row,
                              String labelText, JLabel value, Font font, Color color) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(font);
        lbl.setForeground(Color.BLACK);
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        p.add(lbl, g);
        value.setFont(font.deriveFont(Font.BOLD));
        value.setForeground(color);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        g.gridx = 1; g.weightx = 1;
        p.add(value, g);
    }

    private void bindKey(int keyCode, String name, ActionListener action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.actionPerformed(e); }
        });
    }

    /** Add a greyed-out placeholder text that disappears on focus. */
    private void addPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }
}
