package org.example.ui.transfer;

import org.example.dao.TransferDAO;
import org.example.dao.InventoryDAO;
import org.example.dao.ProductDAO;
import org.example.model.*;
import org.example.ui.UIUtils;
import org.example.db.DatabaseManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Transfer Panel — Quản lý điều chuyển hàng hóa.
 *
 * Tab 1: Yêu cầu chuyển kho → cửa hàng (Store Transfer Request)
 *   Store Manager tạo → submit → Warehouse Supervisor gán kho →
 *   Inventory Manager tạo outbound → Store Manager nhận hàng → tạo inbound.
 *
 * Tab 2: Điều chuyển hàng tồn kho (Warehouse Overstock)
 *   Inventory Manager tạo → submit → Warehouse Supervisor gán cửa hàng →
 *   Store Manager accept → Inventory Manager tạo outbound → Store Manager tạo inbound.
 */
public class TransferPanel extends JPanel {

    private final TransferDAO transferDAO = new TransferDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final ProductDAO productDAO = new ProductDAO();

    // Tab 1: Store Transfer
    private DefaultTableModel storeReqModel;
    private JTable storeReqTable;
    private DefaultTableModel storeItemModel;
    private JTable storeItemTable;

    // Tab 2: Overstock
    private DefaultTableModel overstockModel;
    private JTable overstockTable;
    private DefaultTableModel overstockItemModel;
    private JTable overstockItemTable;

    public TransferPanel() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.COLOR_BG);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(UIUtils.FONT_BOLD);
        tabs.addTab("Yêu cầu chuyển kho → cửa hàng", buildStoreTransferTab());
        tabs.addTab("Điều chuyển hàng tồn kho", buildOverstockTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ═══════════════════════════════════════════════════════════
    //  TAB 1: STORE TRANSFER REQUEST
    // ═══════════════════════════════════════════════════════════
    private JPanel buildStoreTransferTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UIUtils.COLOR_BG);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Top toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);
        JButton btnNew = styledBtn("Tạo yêu cầu", UIUtils.COLOR_PRIMARY, Color.WHITE);
        btnNew.addActionListener(e -> createStoreTransfer());
        toolbar.add(btnNew);
        JButton btnSubmit = styledBtn("Gửi yêu cầu", UIUtils.COLOR_SECONDARY, Color.WHITE);
        btnSubmit.addActionListener(e -> submitStoreTransfer());
        toolbar.add(btnSubmit);
        JButton btnAssign = styledBtn("Gán kho", UIUtils.COLOR_WARNING, Color.WHITE);
        btnAssign.addActionListener(e -> assignWarehouse());
        toolbar.add(btnAssign);
        JButton btnShipped = styledBtn("Đã chuyển", new Color(70, 130, 180), Color.WHITE);
        btnShipped.addActionListener(e -> markShipped());
        toolbar.add(btnShipped);
        JButton btnReceived = styledBtn("Nhận hàng", new Color(60, 150, 60), Color.WHITE);
        btnReceived.addActionListener(e -> markReceived());
        toolbar.add(btnReceived);
        JButton btnReject = styledBtn("Từ chối", UIUtils.COLOR_DANGER, Color.WHITE);
        btnReject.addActionListener(e -> rejectStoreTransfer());
        toolbar.add(btnReject);
        JButton btnRefresh = styledBtn("Tải lại", UIUtils.COLOR_BG, Color.BLACK);
        btnRefresh.setBorder(new LineBorder(UIUtils.COLOR_BORDER));
        btnRefresh.addActionListener(e -> loadStoreTransfers());
        toolbar.add(btnRefresh);
        panel.add(toolbar, BorderLayout.NORTH);

        // Split pane: requests on top, items on bottom
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerLocation(250);
        split.setResizeWeight(0.5);

        // Top: Request list
        storeReqModel = new DefaultTableModel(new String[]{
            "ID", "Số", "Cửa hàng", "Người tạo", "Trạng thái", "Ưu tiên", "Ngày cần", "Kho gán", "Ngày tạo"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        storeReqTable = new JTable(storeReqModel);
        storeReqTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        storeReqTable.setRowHeight(28);
        storeReqTable.setFont(UIUtils.FONT_LABEL);
        storeReqTable.getTableHeader().setFont(UIUtils.FONT_BOLD);
        storeReqTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadStoreTransferItems();
        });
        storeReqTable.getColumnModel().getColumn(0).setMaxWidth(0);
        storeReqTable.getColumnModel().getColumn(0).setMinWidth(0);
        storeReqTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        split.setTopComponent(new JScrollPane(storeReqTable));

        // Bottom: Item list + Add item
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.setOpaque(false);
        JLabel itemLabel = new JLabel("Chi tiết sản phẩm:");
        itemLabel.setFont(UIUtils.FONT_BOLD);
        itemLabel.setForeground(Color.BLACK);
        bottomPanel.add(itemLabel, BorderLayout.NORTH);

        storeItemModel = new DefaultTableModel(new String[]{
            "Item ID", "Sản phẩm ID", "Tên sản phẩm", "SL yêu cầu", "SL duyệt", "Ghi chú"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        storeItemTable = new JTable(storeItemModel);
        storeItemTable.setRowHeight(26);
        storeItemTable.setFont(UIUtils.FONT_LABEL);
        storeItemTable.getTableHeader().setFont(UIUtils.FONT_BOLD);
        storeItemTable.getColumnModel().getColumn(0).setMaxWidth(0);
        storeItemTable.getColumnModel().getColumn(0).setMinWidth(0);
        storeItemTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        storeItemTable.getColumnModel().getColumn(1).setMaxWidth(0);
        storeItemTable.getColumnModel().getColumn(1).setMinWidth(0);
        storeItemTable.getColumnModel().getColumn(1).setPreferredWidth(0);
        bottomPanel.add(new JScrollPane(storeItemTable), BorderLayout.CENTER);

        JPanel itemToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        itemToolbar.setOpaque(false);
        JButton btnAddItem = styledBtn("Thêm SP", UIUtils.COLOR_PRIMARY, Color.WHITE);
        btnAddItem.addActionListener(e -> addStoreTransferItem());
        itemToolbar.add(btnAddItem);
        JButton btnRemoveItem = styledBtn("Xóa SP", UIUtils.COLOR_DANGER, Color.WHITE);
        btnRemoveItem.addActionListener(e -> removeStoreTransferItem());
        itemToolbar.add(btnRemoveItem);
        bottomPanel.add(itemToolbar, BorderLayout.SOUTH);
        split.setBottomComponent(bottomPanel);

        panel.add(split, BorderLayout.CENTER);
        loadStoreTransfers();
        return panel;
    }

    private void loadStoreTransfers() {
        try {
            List<StoreTransferRequest> list = transferDAO.findAllStoreTransfers();
            storeReqModel.setRowCount(0);
            for (StoreTransferRequest r : list) {
                storeReqModel.addRow(new Object[]{
                    r.getRequestId(), r.getRequestNo(), r.getStoreId(),
                    r.getCreatedBy(), r.getStatus(), r.getPriority(),
                    r.getNeedDate(), r.getAssignedWarehouseId(), r.getCreatedAt()
                });
            }
        } catch (SQLException ex) {
            UIUtils.showError(this, "Lỗi tải yêu cầu: " + ex.getMessage());
        }
    }

    private void loadStoreTransferItems() {
        storeItemModel.setRowCount(0);
        int row = storeReqTable.getSelectedRow();
        if (row < 0) return;
        String reqId = (String) storeReqModel.getValueAt(row, 0);
        try {
            List<StoreTransferRequestItem> items = transferDAO.findStoreTransferItems(reqId);
            for (StoreTransferRequestItem item : items) {
                String productName = "";
                try {
                    Product p = productDAO.findById(item.getProductId());
                    if (p != null) productName = p.getName();
                } catch (Exception ignored) {}
                storeItemModel.addRow(new Object[]{
                    item.getItemId(), item.getProductId(), productName,
                    item.getRequestedQty(), item.getApprovedQty(), item.getNote()
                });
            }
        } catch (SQLException ex) {
            UIUtils.showError(this, "Lỗi tải chi tiết: " + ex.getMessage());
        }
    }

    private void createStoreTransfer() {
        JTextField txtStore = new JTextField(20);
        JTextField txtPriority = new JTextField("NORMAL", 10);
        JTextField txtNeedDate = new JTextField(10);
        JTextArea txtNote = new JTextArea(3, 20);
        txtNeedDate.setToolTipText("yyyy-MM-dd");

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.add(new JLabel("Mã cửa hàng:")); form.add(txtStore);
        form.add(new JLabel("Ưu tiên (NORMAL/HIGH/URGENT):")); form.add(txtPriority);
        form.add(new JLabel("Ngày cần (yyyy-MM-dd):")); form.add(txtNeedDate);
        form.add(new JLabel("Ghi chú:")); form.add(new JScrollPane(txtNote));

        int opt = JOptionPane.showConfirmDialog(this, form, "Tạo yêu cầu chuyển kho",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        try {
            StoreTransferRequest r = new StoreTransferRequest();
            r.setStoreId(txtStore.getText().trim());
            r.setCreatedBy("current_user");
            r.setPriority(txtPriority.getText().trim());
            r.setNeedDate(txtNeedDate.getText().trim());
            r.setNote(txtNote.getText().trim());
            r.setStatus("DRAFT");
            transferDAO.createStoreTransferRequest(r);
            loadStoreTransfers();
            UIUtils.showSuccess(this, "Tạo yêu cầu #" + r.getRequestNo() + " thành công!");
        } catch (SQLException ex) {
            UIUtils.showError(this, "Lỗi tạo yêu cầu: " + ex.getMessage());
        }
    }

    private void submitStoreTransfer() {
        int row = storeReqTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu cần gửi."); return; }
        String id = (String) storeReqModel.getValueAt(row, 0);
        String status = (String) storeReqModel.getValueAt(row, 4);
        if (!"DRAFT".equals(status)) { UIUtils.showError(this, "Chỉ gửi yêu cầu ở trạng thái DRAFT."); return; }
        try {
            transferDAO.submitStoreTransfer(id);
            loadStoreTransfers();
            UIUtils.showSuccess(this, "Đã gửi yêu cầu!");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void assignWarehouse() {
        int row = storeReqTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) storeReqModel.getValueAt(row, 0);
        String status = (String) storeReqModel.getValueAt(row, 4);
        if (!"SUBMITTED".equals(status)) { UIUtils.showError(this, "Yêu cầu phải ở trạng thái SUBMITTED."); return; }
        String warehouseId = JOptionPane.showInputDialog(this, "Nhập mã kho gán:", "Gán kho", JOptionPane.PLAIN_MESSAGE);
        if (warehouseId == null || warehouseId.trim().isEmpty()) return;
        try {
            transferDAO.assignWarehouse(id, warehouseId.trim(), "current_user");
            loadStoreTransfers();
            UIUtils.showSuccess(this, "Đã gán kho: " + warehouseId.trim());
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void markShipped() {
        int row = storeReqTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) storeReqModel.getValueAt(row, 0);
        String status = (String) storeReqModel.getValueAt(row, 4);
        if (!"ASSIGNED".equals(status)) { UIUtils.showError(this, "Yêu cầu phải ở trạng thái ASSIGNED."); return; }
        try {
            transferDAO.markShipped(id);
            loadStoreTransfers();
            UIUtils.showSuccess(this, "Đã chuyển hàng!");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void markReceived() {
        int row = storeReqTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) storeReqModel.getValueAt(row, 0);
        String status = (String) storeReqModel.getValueAt(row, 4);
        if (!"SHIPPING".equals(status)) { UIUtils.showError(this, "Yêu cầu phải ở trạng thái SHIPPING."); return; }
        try {
            transferDAO.markReceived(id, "current_user");
            loadStoreTransfers();
            UIUtils.showSuccess(this, "Đã nhận hàng!");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void rejectStoreTransfer() {
        int row = storeReqTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) storeReqModel.getValueAt(row, 0);
        String reason = JOptionPane.showInputDialog(this, "Lý do từ chối:", "Từ chối", JOptionPane.WARNING_MESSAGE);
        if (reason == null) return;
        try {
            transferDAO.rejectStoreTransfer(id, reason);
            loadStoreTransfers();
            UIUtils.showSuccess(this, "Đã từ chối yêu cầu.");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void addStoreTransferItem() {
        int row = storeReqTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu trước."); return; }
        String reqId = (String) storeReqModel.getValueAt(row, 0);
        String status = (String) storeReqModel.getValueAt(row, 4);
        if (!"DRAFT".equals(status)) { UIUtils.showError(this, "Chỉ thêm SP khi trạng thái DRAFT."); return; }

        JTextField txtProduct = new JTextField(20);
        JTextField txtQty = new JTextField("1", 6);
        JTextField txtNote = new JTextField(20);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.add(new JLabel("Mã sản phẩm:")); form.add(txtProduct);
        form.add(new JLabel("Số lượng:")); form.add(txtQty);
        form.add(new JLabel("Ghi chú:")); form.add(txtNote);

        int opt = JOptionPane.showConfirmDialog(this, form, "Thêm sản phẩm vào yêu cầu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        try {
            StoreTransferRequestItem item = new StoreTransferRequestItem();
            item.setRequestId(reqId);
            item.setProductId(txtProduct.getText().trim());
            item.setRequestedQty(Integer.parseInt(txtQty.getText().trim()));
            item.setNote(txtNote.getText().trim());
            transferDAO.addStoreTransferItem(item);
            loadStoreTransferItems();
            UIUtils.showSuccess(this, "Đã thêm sản phẩm.");
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void removeStoreTransferItem() {
        int row = storeItemTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn sản phẩm cần xóa."); return; }
        String itemId = (String) storeItemModel.getValueAt(row, 0);
        try {
            transferDAO.deleteStoreTransferItem(itemId);
            loadStoreTransferItems();
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  TAB 2: WAREHOUSE OVERSTOCK REQUEST
    // ═══════════════════════════════════════════════════════════
    private JPanel buildOverstockTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UIUtils.COLOR_BG);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Top toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);
        JButton btnNew = styledBtn("Tạo yêu cầu", UIUtils.COLOR_PRIMARY, Color.WHITE);
        btnNew.addActionListener(e -> createOverstock());
        toolbar.add(btnNew);
        JButton btnSubmit = styledBtn("Gửi yêu cầu", UIUtils.COLOR_SECONDARY, Color.WHITE);
        btnSubmit.addActionListener(e -> submitOverstock());
        toolbar.add(btnSubmit);
        JButton btnAssignStore = styledBtn("Gán cửa hàng", UIUtils.COLOR_WARNING, Color.WHITE);
        btnAssignStore.addActionListener(e -> assignStore());
        toolbar.add(btnAssignStore);
        JButton btnAccept = styledBtn("Chấp nhận", new Color(60, 150, 60), Color.WHITE);
        btnAccept.addActionListener(e -> acceptOverstock());
        toolbar.add(btnAccept);
        JButton btnShipped = styledBtn("Đã chuyển", new Color(70, 130, 180), Color.WHITE);
        btnShipped.addActionListener(e -> markOverstockShipped());
        toolbar.add(btnShipped);
        JButton btnReceived = styledBtn("Nhận hàng", new Color(100, 170, 60), Color.WHITE);
        btnReceived.addActionListener(e -> markOverstockReceived());
        toolbar.add(btnReceived);
        JButton btnReject = styledBtn("Từ chối", UIUtils.COLOR_DANGER, Color.WHITE);
        btnReject.addActionListener(e -> rejectOverstock());
        toolbar.add(btnReject);
        JButton btnRefresh = styledBtn("Tải lại", UIUtils.COLOR_BG, Color.BLACK);
        btnRefresh.setBorder(new LineBorder(UIUtils.COLOR_BORDER));
        btnRefresh.addActionListener(e -> loadOverstockRequests());
        toolbar.add(btnRefresh);
        panel.add(toolbar, BorderLayout.NORTH);

        // Split pane
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerLocation(250);
        split.setResizeWeight(0.5);

        // Top: Request list
        overstockModel = new DefaultTableModel(new String[]{
            "ID", "Số", "Kho", "Người tạo", "Trạng thái", "Cửa hàng gán", "Ngày tạo"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        overstockTable = new JTable(overstockModel);
        overstockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overstockTable.setRowHeight(28);
        overstockTable.setFont(UIUtils.FONT_LABEL);
        overstockTable.getTableHeader().setFont(UIUtils.FONT_BOLD);
        overstockTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadOverstockItems();
        });
        overstockTable.getColumnModel().getColumn(0).setMaxWidth(0);
        overstockTable.getColumnModel().getColumn(0).setMinWidth(0);
        overstockTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        split.setTopComponent(new JScrollPane(overstockTable));

        // Bottom: Items
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.setOpaque(false);
        JLabel itemLabel = new JLabel("Chi tiết sản phẩm tồn kho:");
        itemLabel.setFont(UIUtils.FONT_BOLD);
        itemLabel.setForeground(Color.BLACK);
        bottomPanel.add(itemLabel, BorderLayout.NORTH);

        overstockItemModel = new DefaultTableModel(new String[]{
            "Item ID", "SP ID", "Tên SP", "SL tồn", "SL chuyển", "Ghi chú"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        overstockItemTable = new JTable(overstockItemModel);
        overstockItemTable.setRowHeight(26);
        overstockItemTable.setFont(UIUtils.FONT_LABEL);
        overstockItemTable.getTableHeader().setFont(UIUtils.FONT_BOLD);
        overstockItemTable.getColumnModel().getColumn(0).setMaxWidth(0);
        overstockItemTable.getColumnModel().getColumn(0).setMinWidth(0);
        overstockItemTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        overstockItemTable.getColumnModel().getColumn(1).setMaxWidth(0);
        overstockItemTable.getColumnModel().getColumn(1).setMinWidth(0);
        overstockItemTable.getColumnModel().getColumn(1).setPreferredWidth(0);
        bottomPanel.add(new JScrollPane(overstockItemTable), BorderLayout.CENTER);

        JPanel itemToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        itemToolbar.setOpaque(false);
        JButton btnAddItem = styledBtn("Thêm SP", UIUtils.COLOR_PRIMARY, Color.WHITE);
        btnAddItem.addActionListener(e -> addOverstockItem());
        itemToolbar.add(btnAddItem);
        JButton btnRemoveItem = styledBtn("Xóa SP", UIUtils.COLOR_DANGER, Color.WHITE);
        btnRemoveItem.addActionListener(e -> removeOverstockItem());
        itemToolbar.add(btnRemoveItem);
        bottomPanel.add(itemToolbar, BorderLayout.SOUTH);
        split.setBottomComponent(bottomPanel);

        panel.add(split, BorderLayout.CENTER);
        loadOverstockRequests();
        return panel;
    }

    private void loadOverstockRequests() {
        try {
            List<WarehouseOverstockRequest> list = transferDAO.findAllOverstockRequests();
            overstockModel.setRowCount(0);
            for (WarehouseOverstockRequest r : list) {
                overstockModel.addRow(new Object[]{
                    r.getRequestId(), r.getRequestNo(), r.getWarehouseId(),
                    r.getCreatedBy(), r.getStatus(), r.getTargetStoreId(), r.getCreatedAt()
                });
            }
        } catch (SQLException ex) {
            UIUtils.showError(this, "Lỗi tải yêu cầu: " + ex.getMessage());
        }
    }

    private void loadOverstockItems() {
        overstockItemModel.setRowCount(0);
        int row = overstockTable.getSelectedRow();
        if (row < 0) return;
        String reqId = (String) overstockModel.getValueAt(row, 0);
        try {
            List<WarehouseOverstockRequestItem> items = transferDAO.findOverstockItems(reqId);
            for (WarehouseOverstockRequestItem item : items) {
                String productName = "";
                try {
                    Product p = productDAO.findById(item.getProductId());
                    if (p != null) productName = p.getName();
                } catch (Exception ignored) {}
                overstockItemModel.addRow(new Object[]{
                    item.getItemId(), item.getProductId(), productName,
                    item.getOverstockQty(), item.getTransferQty(), item.getNote()
                });
            }
        } catch (SQLException ex) {
            UIUtils.showError(this, "Lỗi tải chi tiết: " + ex.getMessage());
        }
    }

    private void createOverstock() {
        JTextField txtWarehouse = new JTextField(20);
        JTextArea txtNote = new JTextArea(3, 20);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.add(new JLabel("Mã kho:")); form.add(txtWarehouse);
        form.add(new JLabel("Ghi chú:")); form.add(new JScrollPane(txtNote));

        int opt = JOptionPane.showConfirmDialog(this, form, "Tạo yêu cầu điều chuyển hàng tồn",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        try {
            WarehouseOverstockRequest r = new WarehouseOverstockRequest();
            r.setWarehouseId(txtWarehouse.getText().trim());
            r.setCreatedBy("current_user");
            r.setNote(txtNote.getText().trim());
            r.setStatus("DRAFT");
            transferDAO.createOverstockRequest(r);
            loadOverstockRequests();
            UIUtils.showSuccess(this, "Tạo yêu cầu #" + r.getRequestNo() + " thành công!");
        } catch (SQLException ex) {
            UIUtils.showError(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void submitOverstock() {
        int row = overstockTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) overstockModel.getValueAt(row, 0);
        try {
            transferDAO.submitOverstockRequest(id);
            loadOverstockRequests();
            UIUtils.showSuccess(this, "Đã gửi yêu cầu!");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void assignStore() {
        int row = overstockTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) overstockModel.getValueAt(row, 0);
        String storeId = JOptionPane.showInputDialog(this, "Nhập mã cửa hàng đích:", "Gán cửa hàng", JOptionPane.PLAIN_MESSAGE);
        if (storeId == null || storeId.trim().isEmpty()) return;
        try {
            transferDAO.assignTargetStore(id, storeId.trim(), "current_user");
            loadOverstockRequests();
            UIUtils.showSuccess(this, "Đã gán cửa hàng: " + storeId.trim());
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void acceptOverstock() {
        int row = overstockTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) overstockModel.getValueAt(row, 0);
        try {
            transferDAO.acceptOverstock(id, "current_user");
            loadOverstockRequests();
            UIUtils.showSuccess(this, "Đã chấp nhận yêu cầu!");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void markOverstockShipped() {
        int row = overstockTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) overstockModel.getValueAt(row, 0);
        try {
            transferDAO.markOverstockShipped(id);
            loadOverstockRequests();
            UIUtils.showSuccess(this, "Đã chuyển hàng!");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void markOverstockReceived() {
        int row = overstockTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) overstockModel.getValueAt(row, 0);
        try {
            transferDAO.markOverstockReceived(id);
            loadOverstockRequests();
            UIUtils.showSuccess(this, "Đã nhận hàng!");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void rejectOverstock() {
        int row = overstockTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu."); return; }
        String id = (String) overstockModel.getValueAt(row, 0);
        String reason = JOptionPane.showInputDialog(this, "Lý do từ chối:", "Từ chối", JOptionPane.WARNING_MESSAGE);
        if (reason == null) return;
        try {
            transferDAO.rejectOverstock(id, reason);
            loadOverstockRequests();
            UIUtils.showSuccess(this, "Đã từ chối.");
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void addOverstockItem() {
        int row = overstockTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn yêu cầu trước."); return; }
        String reqId = (String) overstockModel.getValueAt(row, 0);

        JTextField txtProduct = new JTextField(20);
        JTextField txtOverstockQty = new JTextField("1", 6);
        JTextField txtTransferQty = new JTextField("1", 6);
        JTextField txtNote = new JTextField(20);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.add(new JLabel("Mã sản phẩm:")); form.add(txtProduct);
        form.add(new JLabel("SL tồn kho:")); form.add(txtOverstockQty);
        form.add(new JLabel("SL muốn chuyển:")); form.add(txtTransferQty);
        form.add(new JLabel("Ghi chú:")); form.add(txtNote);

        int opt = JOptionPane.showConfirmDialog(this, form, "Thêm SP tồn kho",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        try {
            WarehouseOverstockRequestItem item = new WarehouseOverstockRequestItem();
            item.setRequestId(reqId);
            item.setProductId(txtProduct.getText().trim());
            item.setOverstockQty(Integer.parseInt(txtOverstockQty.getText().trim()));
            item.setTransferQty(Integer.parseInt(txtTransferQty.getText().trim()));
            item.setNote(txtNote.getText().trim());
            transferDAO.addOverstockItem(item);
            loadOverstockItems();
            UIUtils.showSuccess(this, "Đã thêm SP.");
        } catch (Exception ex) {
            UIUtils.showError(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void removeOverstockItem() {
        int row = overstockItemTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn SP cần xóa."); return; }
        String itemId = (String) overstockItemModel.getValueAt(row, 0);
        try {
            transferDAO.deleteOverstockItem(itemId);
            loadOverstockItems();
        } catch (SQLException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════
    private JButton styledBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(UIUtils.FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int textWidth = b.getFontMetrics(b.getFont()).stringWidth(text);
        b.setPreferredSize(new Dimension(Math.max(textWidth + 36, 80), 32));
        b.setMinimumSize(new Dimension(Math.max(textWidth + 36, 80), 32));
        return b;
    }
}

