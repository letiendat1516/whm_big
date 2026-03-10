package org.example.ui.pos;

import org.example.dao.OrderDAO;
import org.example.model.*;
import org.example.service.POSService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Payment & Order Management Panel — Quản lý đơn hàng, thanh toán, công nợ, trả hàng.
 *
 * Tabs:
 *   1. Đơn hàng (Order History) — search, filter by date/status
 *   2. Công nợ  (Debt Management) — partial payment collection
 *   3. Trả hàng (Returns) — process returns against completed orders
 */
public class PaymentPanel extends JPanel {

    private final OrderDAO orderDAO = new OrderDAO();
    private final POSService posService = new POSService();

    // ── Order History tab ──
    private final DefaultTableModel orderModel = new DefaultTableModel(
            new String[]{"Mã đơn", "Ngày", "Khách hàng", "Tổng tiền", "Giảm giá", "Thuế",
                          "Thanh toán", "Đã trả", "Còn nợ", "TT Thanh toán", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblOrders = new JTable(orderModel);
    private final JTextField txtOrderSearch = new JTextField(18);
    private final JComboBox<String> cboDateFilter = new JComboBox<>(new String[]{
            "Hôm nay", "7 ngày qua", "30 ngày qua", "Tất cả"});

    // ── Debt tab ──
    private final DefaultTableModel debtModel = new DefaultTableModel(
            new String[]{"Mã đơn", "Ngày", "Khách hàng", "Tổng đơn", "Đã trả", "Còn nợ", "orderId"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblDebt = new JTable(debtModel);

    // ── Return tab ──
    private final DefaultTableModel returnModel = new DefaultTableModel(
            new String[]{"Mã trả hàng", "Mã đơn gốc", "Ngày trả", "Số lượng", "Trạng thái", "Khách hàng"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblReturns = new JTable(returnModel);

    // ── Order detail ──
    private final DefaultTableModel detailModel = new DefaultTableModel(
            new String[]{"Sản phẩm", "Biến thể", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblDetail = new JTable(detailModel);

    public PaymentPanel() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.COLOR_BG);

        // Header
        JLabel title = UIUtils.sectionLabel("  QUẢN LÝ THANH TOÁN & ĐƠN HÀNG");
        title.setFont(UIUtils.FONT_LARGE);
        title.setBackground(UIUtils.COLOR_CARD);
        title.setOpaque(true);
        title.setForeground(Color.BLACK);
        title.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.COLOR_BORDER),
                new EmptyBorder(10, 12, 10, 12)));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIUtils.FONT_BOLD);
        tabs.setForeground(Color.BLACK);
        tabs.addTab("Đơn hàng", buildOrderTab());
        tabs.addTab("Công nợ", buildDebtTab());
        tabs.addTab("Trả hàng", buildReturnTab());
        add(tabs, BorderLayout.CENTER);

        loadOrders();
    }

    // ══════════════════════════════════════════════════════════════
    //  TAB 1: ĐƠN HÀNG
    // ══════════════════════════════════════════════════════════════
    private JPanel buildOrderTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(6, 10, 6, 10)));

        JLabel lblSearch = new JLabel("Tìm:");
        lblSearch.setFont(UIUtils.FONT_BOLD);
        lblSearch.setForeground(Color.BLACK);
        toolbar.add(lblSearch);
        addPlaceholder(txtOrderSearch, "Mã đơn, tên KH...");
        toolbar.add(txtOrderSearch);

        JLabel lblDate = new JLabel("Lọc:");
        lblDate.setFont(UIUtils.FONT_BOLD);
        lblDate.setForeground(Color.BLACK);
        toolbar.add(lblDate);
        toolbar.add(cboDateFilter);

        JButton btnSearch = UIUtils.primaryButton("Tìm kiếm");
        btnSearch.addActionListener(e -> loadOrders());
        txtOrderSearch.addActionListener(e -> loadOrders());
        cboDateFilter.addActionListener(e -> loadOrders());
        toolbar.add(btnSearch);

        JButton btnViewDetail = UIUtils.secondaryButton("Xem chi tiết");
        btnViewDetail.addActionListener(e -> viewOrderDetail());
        toolbar.add(btnViewDetail);

        JButton btnRefresh = UIUtils.secondaryButton("Làm mới");
        btnRefresh.addActionListener(e -> { txtOrderSearch.setText(""); loadOrders(); });
        toolbar.add(btnRefresh);

        p.add(toolbar, BorderLayout.NORTH);

        // Split: orders top, detail bottom
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.6);

        UIUtils.applyZebraRenderer(tblOrders);
        tblOrders.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblOrders.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) viewOrderDetail();
        });
        split.setTopComponent(new JScrollPane(tblOrders));

        // Detail panel
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(UIUtils.COLOR_CARD);
        JLabel lblDetail = new JLabel("  Chi tiết đơn hàng:");
        lblDetail.setFont(UIUtils.FONT_BOLD);
        lblDetail.setForeground(Color.BLACK);
        lblDetail.setBorder(new EmptyBorder(6, 6, 6, 6));
        detailPanel.add(lblDetail, BorderLayout.NORTH);
        UIUtils.applyZebraRenderer(tblDetail);
        detailPanel.add(new JScrollPane(tblDetail), BorderLayout.CENTER);
        split.setBottomComponent(detailPanel);

        p.add(split, BorderLayout.CENTER);

        // Stats
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stats.setBackground(new Color(232, 245, 233));
        JLabel lblHint = new JLabel("Chọn đơn hàng để xem chi tiết. Đơn có công nợ chuyển qua tab 'Công nợ' để thu tiền.");
        lblHint.setFont(UIUtils.FONT_LABEL);
        lblHint.setForeground(Color.BLACK);
        stats.add(lblHint);
        p.add(stats, BorderLayout.SOUTH);

        return p;
    }

    // ══════════════════════════════════════════════════════════════
    //  TAB 2: CÔNG NỢ
    // ══════════════════════════════════════════════════════════════
    private JPanel buildDebtTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(6, 10, 6, 10)));

        JButton btnLoad = UIUtils.primaryButton("Tải danh sách nợ");
        btnLoad.addActionListener(e -> loadDebtOrders());
        toolbar.add(btnLoad);

        JButton btnPayDebt = UIUtils.successButton("Thu tiền nợ");
        btnPayDebt.addActionListener(e -> collectDebt());
        toolbar.add(btnPayDebt);

        JButton btnRefresh = UIUtils.secondaryButton("Làm mới");
        btnRefresh.addActionListener(e -> loadDebtOrders());
        toolbar.add(btnRefresh);

        p.add(toolbar, BorderLayout.NORTH);

        UIUtils.applyZebraRenderer(tblDebt);
        // Hide orderId column
        tblDebt.getColumnModel().getColumn(6).setMinWidth(0);
        tblDebt.getColumnModel().getColumn(6).setMaxWidth(0);
        tblDebt.getColumnModel().getColumn(6).setWidth(0);
        tblDebt.getColumnModel().getColumn(6).setPreferredWidth(0);
        p.add(new JScrollPane(tblDebt), BorderLayout.CENTER);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stats.setBackground(new Color(255, 243, 224));
        JLabel lblHint = new JLabel("Chọn đơn hàng còn nợ → 'Thu tiền nợ' để nhận thanh toán bổ sung.");
        lblHint.setFont(UIUtils.FONT_LABEL);
        lblHint.setForeground(Color.BLACK);
        stats.add(lblHint);
        p.add(stats, BorderLayout.SOUTH);

        loadDebtOrders();
        return p;
    }

    // ══════════════════════════════════════════════════════════════
    //  TAB 3: TRẢ HÀNG
    // ══════════════════════════════════════════════════════════════
    private JPanel buildReturnTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(6, 10, 6, 10)));

        JButton btnLoad = UIUtils.primaryButton("Tải DS trả hàng");
        btnLoad.addActionListener(e -> loadReturns());
        toolbar.add(btnLoad);

        JButton btnNewReturn = UIUtils.successButton("Tạo phiếu trả hàng");
        btnNewReturn.addActionListener(e -> openReturnDialog());
        toolbar.add(btnNewReturn);

        JButton btnViewItems = UIUtils.secondaryButton("Xem chi tiết");
        btnViewItems.addActionListener(e -> viewReturnItems());
        toolbar.add(btnViewItems);

        p.add(toolbar, BorderLayout.NORTH);

        UIUtils.applyZebraRenderer(tblReturns);
        p.add(new JScrollPane(tblReturns), BorderLayout.CENTER);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stats.setBackground(new Color(252, 228, 236));
        JLabel lblHint = new JLabel("Tạo phiếu trả hàng từ đơn hàng đã hoàn thành. Hàng trả sẽ được nhập lại kho.");
        lblHint.setFont(UIUtils.FONT_LABEL);
        lblHint.setForeground(Color.BLACK);
        stats.add(lblHint);
        p.add(stats, BorderLayout.SOUTH);

        loadReturns();
        return p;
    }

    // ══════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ══════════════════════════════════════════════════════════════
    private void loadOrders() {
        orderModel.setRowCount(0);
        try {
            String search = txtOrderSearch.getText().trim();
            if (search.equals("Mã đơn, tên KH...")) search = "";

            List<Order> orders;
            if (!search.isEmpty()) {
                orders = orderDAO.searchOrders(search);
            } else {
                String dateFilter = (String) cboDateFilter.getSelectedItem();
                String date = switch (dateFilter) {
                    case "Hôm nay" -> LocalDate.now().toString();
                    case "7 ngày qua" -> LocalDate.now().minusDays(7).toString();
                    case "30 ngày qua" -> LocalDate.now().minusDays(30).toString();
                    default -> null;
                };
                if (date != null && !"Hôm nay".equals(dateFilter)) {
                    orders = orderDAO.searchOrders(""); // get all, filter in view
                } else if (date != null) {
                    orders = orderDAO.findOrdersByDate(date);
                } else {
                    orders = orderDAO.searchOrders("");
                }
            }
            for (Order o : orders) {
                orderModel.addRow(new Object[]{
                        o.getOrderId().substring(0, Math.min(8, o.getOrderId().length())).toUpperCase(),
                        o.getOrderDate() != null ? o.getOrderDate().substring(0, Math.min(16, o.getOrderDate().length())) : "",
                        o.getCustomerName() != null ? o.getCustomerName() : "Khách lẻ",
                        UIUtils.formatCurrency(o.getTotalAmount()),
                        UIUtils.formatCurrency(o.getDiscountAmount()),
                        UIUtils.formatCurrency(o.getTaxAmount()),
                        UIUtils.formatCurrency(o.getFinalAmount()),
                        UIUtils.formatCurrency(o.getPaidAmount()),
                        UIUtils.formatCurrency(o.getDebtAmount()),
                        translatePaymentStatus(o.getPaymentStatus()),
                        translateStatus(o.getStatus())
                });
            }
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi tải đơn hàng: " + ex.getMessage());
        }
    }

    private void loadDebtOrders() {
        debtModel.setRowCount(0);
        try {
            List<Order> orders = orderDAO.findDebtOrders();
            for (Order o : orders) {
                debtModel.addRow(new Object[]{
                        o.getOrderId().substring(0, Math.min(8, o.getOrderId().length())).toUpperCase(),
                        o.getOrderDate() != null ? o.getOrderDate().substring(0, Math.min(16, o.getOrderDate().length())) : "",
                        o.getCustomerName() != null ? o.getCustomerName() : "Khách lẻ",
                        UIUtils.formatCurrency(o.getFinalAmount()),
                        UIUtils.formatCurrency(o.getPaidAmount()),
                        UIUtils.formatCurrency(o.getDebtAmount()),
                        o.getOrderId()
                });
            }
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi tải danh sách nợ: " + ex.getMessage());
        }
    }

    private void loadReturns() {
        returnModel.setRowCount(0);
        try {
            List<ReturnOrder> returns = orderDAO.findAllReturns();
            for (ReturnOrder r : returns) {
                returnModel.addRow(new Object[]{
                        r.getReturnId() != null ? r.getReturnId().substring(0, Math.min(8, r.getReturnId().length())).toUpperCase() : "",
                        r.getOrderId() != null ? r.getOrderId().substring(0, Math.min(8, r.getOrderId().length())).toUpperCase() : "",
                        r.getReturnDate(),
                        r.getQuantity(),
                        translateReturnStatus(r.getStatus()),
                        r.getCustomerName() != null ? r.getCustomerName() : "Khách lẻ"
                });
            }
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi tải DS trả hàng: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ══════════════════════════════════════════════════════════════
    private void viewOrderDetail() {
        int row = tblOrders.getSelectedRow();
        if (row < 0) return;
        detailModel.setRowCount(0);
        try {
            // Get full orderId from search
            String shortId = (String) orderModel.getValueAt(row, 0);
            List<Order> orders = orderDAO.searchOrders(shortId);
            if (orders.isEmpty()) return;
            Order order = orders.get(0);
            List<OrderItem> items = orderDAO.findItemsByOrder(order.getOrderId());
            for (OrderItem item : items) {
                detailModel.addRow(new Object[]{
                        item.getProductName(),
                        item.getVariantName(),
                        item.getBarcode(),
                        item.getQuantity(),
                        UIUtils.formatCurrency(item.getUnitPrice()),
                        UIUtils.formatCurrency(item.getSubtotal())
                });
            }
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void collectDebt() {
        int row = tblDebt.getSelectedRow();
        if (row < 0) {
            UIUtils.showError(this, "Chọn đơn hàng cần thu nợ.");
            return;
        }
        String orderId = (String) debtModel.getValueAt(row, 6);
        String debtStr = (String) debtModel.getValueAt(row, 5);

        // Input dialog
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblDebt = new JLabel("Còn nợ:");
        lblDebt.setForeground(Color.RED);
        lblDebt.setFont(UIUtils.FONT_BOLD);
        form.add(lblDebt);
        JLabel lblDebtVal = new JLabel(debtStr);
        lblDebtVal.setForeground(Color.RED);
        lblDebtVal.setFont(UIUtils.FONT_BOLD);
        form.add(lblDebtVal);

        form.add(new JLabel("Số tiền trả:"));
        JTextField txtAmount = new JTextField();
        form.add(txtAmount);

        form.add(new JLabel("Phương thức:"));
        JComboBox<String> cboMethod = new JComboBox<>(new String[]{"CASH", "QR", "CARD"});
        form.add(cboMethod);

        int res = JOptionPane.showConfirmDialog(this, form, "Thu tiền nợ", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            String amtStr = txtAmount.getText().trim().replace(",", "").replace(".", "");
            double amount = Double.parseDouble(amtStr);
            if (amount <= 0) {
                UIUtils.showError(this, "Số tiền phải > 0.");
                return;
            }
            String method = (String) cboMethod.getSelectedItem();
            posService.payDebt(orderId, method, amount);
            UIUtils.showSuccess(this, "Thu nợ thành công: " + UIUtils.formatCurrency(amount));
            loadDebtOrders();
            loadOrders();
        } catch (NumberFormatException e) {
            UIUtils.showError(this, "Số tiền không hợp lệ.");
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi thu nợ: " + ex.getMessage());
        }
    }

    private void openReturnDialog() {
        // Step 1: Ask for order ID
        String inputOrderId = JOptionPane.showInputDialog(this,
                "Nhập mã đơn hàng gốc (8 ký tự đầu):", "Tạo phiếu trả hàng", JOptionPane.QUESTION_MESSAGE);
        if (inputOrderId == null || inputOrderId.trim().isEmpty()) return;

        try {
            List<Order> orders = orderDAO.searchOrders(inputOrderId.trim());
            if (orders.isEmpty()) {
                UIUtils.showError(this, "Không tìm thấy đơn hàng: " + inputOrderId);
                return;
            }

            // Find completed order
            Order order = null;
            for (Order o : orders) {
                if ("COMPLETED".equals(o.getStatus())) { order = o; break; }
            }
            if (order == null) {
                UIUtils.showError(this, "Đơn hàng chưa hoàn thành, không thể trả hàng.");
                return;
            }

            // Step 2: Show items to select for return
            List<OrderItem> items = orderDAO.findItemsByOrder(order.getOrderId());
            if (items.isEmpty()) {
                UIUtils.showError(this, "Đơn hàng không có sản phẩm.");
                return;
            }

            // Build return selection dialog
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setPreferredSize(new Dimension(600, 350));

            JLabel info = new JLabel(String.format("Đơn hàng: %s | Khách: %s | Ngày: %s",
                    order.getOrderId().substring(0, 8).toUpperCase(),
                    order.getCustomerName() != null ? order.getCustomerName() : "Khách lẻ",
                    order.getOrderDate()));
            info.setFont(UIUtils.FONT_BOLD);
            info.setForeground(Color.BLACK);
            panel.add(info, BorderLayout.NORTH);

            DefaultTableModel retItemModel = new DefaultTableModel(
                    new String[]{"Chọn", "Sản phẩm", "Biến thể", "SL đã mua", "SL trả", "Lý do"}, 0) {
                @Override public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : String.class; }
                @Override public boolean isCellEditable(int r, int c) { return c == 0 || c == 4 || c == 5; }
            };
            for (OrderItem item : items) {
                retItemModel.addRow(new Object[]{
                        false,
                        item.getProductName(),
                        item.getVariantName(),
                        String.valueOf(item.getQuantity()),
                        "1",
                        "Hàng lỗi"
                });
            }
            JTable tblRetItems = new JTable(retItemModel);
            UIUtils.applyZebraRenderer(tblRetItems);
            panel.add(new JScrollPane(tblRetItems), BorderLayout.CENTER);

            int res = JOptionPane.showConfirmDialog(this, panel,
                    "Chọn sản phẩm trả hàng", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            // Collect selected items
            java.util.List<ReturnOrderItem> returnItems = new java.util.ArrayList<>();
            for (int i = 0; i < retItemModel.getRowCount(); i++) {
                Boolean selected = (Boolean) retItemModel.getValueAt(i, 0);
                if (selected != null && selected) {
                    int returnQty;
                    try {
                        returnQty = Integer.parseInt(retItemModel.getValueAt(i, 4).toString().trim());
                    } catch (NumberFormatException e) {
                        returnQty = 1;
                    }
                    int origQty = items.get(i).getQuantity();
                    if (returnQty > origQty) returnQty = origQty;
                    if (returnQty <= 0) continue;

                    ReturnOrderItem ri = new ReturnOrderItem();
                    ri.setOrderItemId(items.get(i).getOrderItemId());
                    ri.setProductId(items.get(i).getProductId());
                    ri.setVariantId(items.get(i).getVariantId());
                    ri.setQuantity(returnQty);
                    ri.setUnitPrice(items.get(i).getUnitPrice());
                    ri.setReason(retItemModel.getValueAt(i, 5).toString());
                    returnItems.add(ri);
                }
            }
            if (returnItems.isEmpty()) {
                UIUtils.showError(this, "Chưa chọn sản phẩm nào để trả.");
                return;
            }

            // Process return
            posService.processReturn(order.getOrderId(), returnItems, null, null);

            double totalRefund = returnItems.stream()
                    .mapToDouble(r -> r.getQuantity() * r.getUnitPrice()).sum();

            UIUtils.showSuccess(this, String.format(
                    "Trả hàng thành công!\nSố SP trả: %d\nTổng hoàn: %s",
                    returnItems.size(), UIUtils.formatCurrency(totalRefund)));

            loadReturns();
            loadOrders();

        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi trả hàng: " + ex.getMessage());
        }
    }

    private void viewReturnItems() {
        int row = tblReturns.getSelectedRow();
        if (row < 0) {
            UIUtils.showError(this, "Chọn phiếu trả hàng.");
            return;
        }
        try {
            // Get full returnId
            String shortId = (String) returnModel.getValueAt(row, 0);
            List<ReturnOrder> returns = orderDAO.findAllReturns();
            ReturnOrder found = null;
            for (ReturnOrder r : returns) {
                if (r.getReturnId() != null && r.getReturnId().toUpperCase().startsWith(shortId)) {
                    found = r;
                    break;
                }
            }
            if (found == null) {
                UIUtils.showError(this, "Không tìm thấy phiếu trả hàng.");
                return;
            }

            List<ReturnOrderItem> items = orderDAO.findReturnItems(found.getReturnId());
            if (items.isEmpty()) {
                UIUtils.showError(this, "Phiếu trả hàng không có sản phẩm.");
                return;
            }

            // Show in dialog
            DefaultTableModel mdl = new DefaultTableModel(
                    new String[]{"Sản phẩm", "Biến thể", "Số lượng", "Đơn giá", "Thành tiền", "Lý do"}, 0);
            for (ReturnOrderItem ri : items) {
                mdl.addRow(new Object[]{
                        ri.getProductName(), ri.getVariantName(), ri.getQuantity(),
                        UIUtils.formatCurrency(ri.getUnitPrice()),
                        UIUtils.formatCurrency(ri.getSubtotal()),
                        ri.getReason()
                });
            }
            JTable tbl = new JTable(mdl);
            UIUtils.applyZebraRenderer(tbl);
            JScrollPane sp = new JScrollPane(tbl);
            sp.setPreferredSize(new Dimension(600, 250));
            JOptionPane.showMessageDialog(this, sp, "Chi tiết trả hàng: " + shortId, JOptionPane.PLAIN_MESSAGE);

        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════
    private String translateStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "PENDING" -> "Đang xử lý";
            case "CONFIRMED" -> "Đã xác nhận";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            case "REFUNDED" -> "Đã hoàn";
            default -> s;
        };
    }

    private String translatePaymentStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "PAID" -> "Đã thanh toán";
            case "PARTIAL" -> "Trả một phần";
            case "UNPAID" -> "Chưa thanh toán";
            default -> s;
        };
    }

    private String translateReturnStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "PENDING" -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "COMPLETED" -> "Hoàn tất";
            case "REJECTED" -> "Từ chối";
            default -> s;
        };
    }

    private void addPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }
}

