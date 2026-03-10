package org.example.ui.inventory;

import org.example.model.Warehouse;
import org.example.model.WarehouseBalance;
import org.example.service.InventoryService;
import org.example.service.ProductService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module 2 — Inventory Panel (Kho hàng)
 * Tabs: Tồn kho | Nhập hàng | Cảnh báo
 */
public class InventoryPanel extends JPanel {

    private final InventoryService inventoryService = new InventoryService();
    private final ProductService productService = new ProductService();

    // Stock table
    private final DefaultTableModel stockModel = new DefaultTableModel(
            new String[]{"Kho", "Mã SP", "Tên sản phẩm", "Tồn thực", "Đặt giữ", "Khả dụng", "Ngưỡng"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblStock = new JTable(stockModel);
    private JComboBox<String> cboWarehouse;
    private final JLabel lblLowCount = new JLabel("0 cảnh báo");

    // Low stock table
    private final DefaultTableModel lowModel = new DefaultTableModel(
            new String[]{"Kho", "Tên sản phẩm", "Tồn", "Ngưỡng", "Thiếu"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblLow = new JTable(lowModel);

    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.COLOR_BG);

        JLabel title = UIUtils.sectionLabel("  QUẢN LÝ KHO HÀNG");
        title.setFont(UIUtils.FONT_LARGE);
        title.setBackground(UIUtils.COLOR_CARD);
        title.setOpaque(true);
        title.setForeground(Color.BLACK);
        title.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.COLOR_BORDER),
            new EmptyBorder(10, 12, 10, 12)
        ));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIUtils.FONT_BOLD);
        tabs.setForeground(Color.BLACK);
        tabs.addTab("Tồn kho", buildStockTab());
        tabs.addTab("Nhập hàng", buildInboundTab());
        tabs.addTab("Cảnh báo", buildAlertTab());
        add(tabs, BorderLayout.CENTER);

        loadStockData();
    }

    private JPanel buildStockTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        cboWarehouse = new JComboBox<>();
        cboWarehouse.addItem("Tất cả kho");
        loadWarehouses();
        cboWarehouse.addActionListener(e -> loadStockData());
        JLabel lblKho = new JLabel("Kho:");
        lblKho.setFont(UIUtils.FONT_BOLD);
        lblKho.setForeground(Color.BLACK);
        toolbar.add(lblKho);
        toolbar.add(cboWarehouse);
        JButton btnRefresh = UIUtils.secondaryButton("Làm mới");
        btnRefresh.addActionListener(e -> loadStockData());
        toolbar.add(btnRefresh);
        lblLowCount.setFont(UIUtils.FONT_BOLD);
        lblLowCount.setForeground(UIUtils.COLOR_DANGER);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(lblLowCount);
        p.add(toolbar, BorderLayout.NORTH);

        // Table with color for low stock
        UIUtils.applyZebraRenderer(tblStock);
        tblStock.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    Object avail = t.getModel().getValueAt(row, 5);
                    Object thresh = t.getModel().getValueAt(row, 6);
                    try {
                        int a = Integer.parseInt(avail != null ? avail.toString() : "0");
                        int th = Integer.parseInt(thresh != null ? thresh.toString() : "0");
                        c.setBackground(a <= th ? UIUtils.COLOR_LOW_STOCK : (row%2==0 ? Color.WHITE : UIUtils.COLOR_TABLE_EVEN));
                    } catch (NumberFormatException ignored) {}
                }
                return c;
            }
        });
        p.add(new JScrollPane(tblStock), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildInboundTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Use a simple card panel instead of titledPanel to avoid layout overlap
        JPanel formOuter = new JPanel(new BorderLayout(0, 8));
        formOuter.setBackground(UIUtils.COLOR_CARD);
        formOuter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 0, 0, UIUtils.COLOR_PRIMARY),
            BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)
            )
        ));

        JLabel formTitle = new JLabel("Nhập hàng vào kho");
        formTitle.setFont(UIUtils.FONT_BOLD);
        formTitle.setForeground(UIUtils.COLOR_PRIMARY);
        formTitle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER),
            new EmptyBorder(0, 0, 8, 0)
        ));
        formOuter.add(formTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIUtils.COLOR_CARD);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Warehouse
        JComboBox<Object> cboW = new JComboBox<>();
        cboW.addItem("-- Chọn kho --");
        try {
            for (Warehouse w : inventoryService.getWarehouses()) cboW.addItem(w);
        } catch (Exception ignored) {}

        // Product
        JTextField txtProduct = new JTextField(20);
        JTextField txtProductId = new JTextField();
        txtProductId.setVisible(false);
        JButton btnPickProduct = UIUtils.secondaryButton("Chọn SP");

        // Qty
        JSpinner spinQty = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
        JTextArea txtNote = new JTextArea(3, 20);

        int row = 0;
        JLabel l1 = new JLabel("Kho nhập (*)"); l1.setFont(UIUtils.FONT_BOLD); l1.setForeground(Color.BLACK);
        g.gridx=0; g.gridy=row; form.add(l1, g);
        g.gridx=1; form.add(cboW, g);
        row++;
        JLabel l2 = new JLabel("Sản phẩm (*)"); l2.setFont(UIUtils.FONT_BOLD); l2.setForeground(Color.BLACK);
        g.gridx=0; g.gridy=row; form.add(l2, g);
        JPanel prodRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        prodRow.setBackground(Color.WHITE);
        prodRow.add(txtProduct); prodRow.add(btnPickProduct);
        g.gridx=1; form.add(prodRow, g);
        row++;
        JLabel l3 = new JLabel("Số lượng (*)"); l3.setFont(UIUtils.FONT_BOLD); l3.setForeground(Color.BLACK);
        g.gridx=0; g.gridy=row; form.add(l3, g);
        g.gridx=1; form.add(spinQty, g);
        row++;
        JLabel l4 = new JLabel("Ghi chú"); l4.setFont(UIUtils.FONT_BOLD); l4.setForeground(Color.BLACK);
        g.gridx=0; g.gridy=row; form.add(l4, g);
        g.gridx=1; form.add(new JScrollPane(txtNote), g);
        row++;

        JButton btnSave = UIUtils.successButton("Xác nhận nhập");
        g.gridx=0; g.gridy=row; g.gridwidth=2; form.add(btnSave, g);

        // Product picker
        btnPickProduct.addActionListener(e -> {
            try {
                var products = productService.getAllActive();
                Object[] opts = products.stream()
                    .map(pr -> pr.getName() + " [" + pr.getProductCode() + "]").toArray();
                String sel = (String) JOptionPane.showInputDialog(this, "Chọn sản phẩm:", "Tìm sản phẩm",
                    JOptionPane.PLAIN_MESSAGE, null, opts, opts.length > 0 ? opts[0] : null);
                if (sel != null) {
                    int idx = java.util.Arrays.asList(opts).indexOf(sel);
                    txtProduct.setText(sel);
                    txtProductId.setText(products.get(idx).getProductId());
                }
            } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        });

        btnSave.addActionListener(e -> {
            if (cboW.getSelectedIndex() == 0 || txtProductId.getText().isEmpty()) {
                UIUtils.showError(this, "Vui lòng chọn kho và sản phẩm."); return;
            }
            Warehouse wh = (Warehouse) cboW.getSelectedItem();
            int qty = (int) spinQty.getValue();
            try {
                inventoryService.receiveStock(wh.getWarehouseId(), txtProductId.getText(), qty);
                UIUtils.showSuccess(this, "Nhập kho thành công! +" + qty + " sản phẩm.");
                loadStockData();
                txtProduct.setText(""); txtProductId.setText("");
                spinQty.setValue(1); txtNote.setText("");
            } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        });

        formOuter.add(form, BorderLayout.CENTER);
        p.add(formOuter, BorderLayout.NORTH);
        return p;
    }

    private JPanel buildAlertTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        JButton btnRefresh = UIUtils.secondaryButton("Tải lại cảnh báo");
        btnRefresh.addActionListener(e -> loadAlerts());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnRefresh);
        top.add(lblLowCount);
        p.add(top, BorderLayout.NORTH);
        UIUtils.applyZebraRenderer(tblLow);
        tblLow.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) comp.setBackground(UIUtils.COLOR_LOW_STOCK);
                return comp;
            }
        });
        p.add(new JScrollPane(tblLow), BorderLayout.CENTER);
        loadAlerts();
        return p;
    }

    private void loadWarehouses() {
        try {
            for (Warehouse w : inventoryService.getWarehouses()) cboWarehouse.addItem("[" + w.getWarehouseCode() + "] " + w.getWarehouseName());
        } catch (Exception ignored) {}
    }

    private void loadStockData() {
        stockModel.setRowCount(0);
        try {
            List<WarehouseBalance> balances = inventoryService.getAllStock();

            // Filter by selected warehouse
            String selectedWarehouse = (String) cboWarehouse.getSelectedItem();
            if (selectedWarehouse != null && !"Tất cả kho".equals(selectedWarehouse)) {
                // Extract warehouse code from format "[CODE] Name"
                balances = balances.stream()
                    .filter(b -> selectedWarehouse.contains(b.getWarehouseName()))
                    .collect(Collectors.toList());
            }

            int lowCount = 0;
            for (WarehouseBalance b : balances) {
                int avail = b.getAvailableQty();
                if (avail <= b.getThreshold()) lowCount++;
                stockModel.addRow(new Object[]{
                    b.getWarehouseName(), b.getProductCode(), b.getProductName(),
                    b.getOnHandQty(), b.getReservedQty(), avail, b.getThreshold()
                });
            }
            lblLowCount.setText(lowCount > 0 ? "⚠ " + lowCount + " SP sắp hết hàng" : "✔ Tồn kho ổn định");
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void loadAlerts() {
        lowModel.setRowCount(0);
        try {
            List<WarehouseBalance> alerts = inventoryService.getLowStockAlerts();
            lblLowCount.setText("⚠ " + alerts.size() + " sản phẩm cần bổ sung");
            for (WarehouseBalance b : alerts) {
                int shortage = b.getThreshold() - b.getAvailableQty();
                lowModel.addRow(new Object[]{
                    b.getWarehouseName(), b.getProductName(),
                    b.getAvailableQty(), b.getThreshold(), shortage
                });
            }
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }
}

