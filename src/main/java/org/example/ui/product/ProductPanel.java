package org.example.ui.product;

import org.example.model.*;
import org.example.service.ProductService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 3 — Product Panel (Sản phẩm, Biến thể, Giá, Danh mục)
 * Redesigned with WMS-inspired clean card-based layout.
 */
public class ProductPanel extends JPanel {

    private final ProductService service = new ProductService();

    // ── Products table ──
    private final DefaultTableModel prodModel = new DefaultTableModel(
            new String[]{"Mã SP", "Tên sản phẩm", "Danh mục", "Thương hiệu", "Đơn vị", "Trạng thái", "ProductID"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblProducts = new JTable(prodModel);
    private final JTextField txtProdSearch = new JTextField(20);
    private final JLabel lblProdStats = new JLabel("Tổng: 0 sản phẩm");

    // ── Variants & Price ──
    private final DefaultTableModel varModel = new DefaultTableModel(
            new String[]{"Tên biến thể", "Barcode", "Giá hiện tại", "Trạng thái", "variant_id"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblVariants = new JTable(varModel);
    private final JLabel lblSelectedProd = new JLabel("Chưa chọn sản phẩm");

    // ── Categories table ──
    private final DefaultTableModel catModel = new DefaultTableModel(
            new String[]{"Mã danh mục", "Tên danh mục", "Mô tả", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblCats = new JTable(catModel);
    private final JLabel lblCatStats = new JLabel("Tổng: 0 danh mục");

    public ProductPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.COLOR_BG);

        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIUtils.FONT_BOLD);
        tabs.setForeground(Color.BLACK);
        tabs.addTab("Sản phẩm", buildProductsTab());
        tabs.addTab("Biến thể & Giá", buildVariantPriceTab());
        tabs.addTab("Danh mục", buildCategoryTab());
        add(tabs, BorderLayout.CENTER);

        loadProducts();
        loadCategories();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.COLOR_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.COLOR_BORDER),
            new EmptyBorder(12, 16, 12, 16)
        ));
        JLabel title = new JLabel("QUẢN LÝ SẢN PHẨM & GIÁ");
        title.setFont(UIUtils.FONT_LARGE);
        title.setForeground(Color.BLACK);
        header.add(title, BorderLayout.WEST);
        return header;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 1: SẢN PHẨM
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildProductsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIUtils.COLOR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(8, 10, 8, 10)));

        toolbar.add(styledLabel("Tìm kiếm:"));
        styleTextField(txtProdSearch);
        txtProdSearch.setToolTipText("Nhập mã SP, tên hoặc thương hiệu");
        addPlaceholder(txtProdSearch, "Mã SP, tên, thương hiệu...");
        txtProdSearch.addActionListener(e -> loadProducts());
        toolbar.add(txtProdSearch);
        toolbar.add(btn("Tìm", UIUtils.COLOR_PRIMARY, e -> loadProducts()));
        toolbar.add(vertSep());
        toolbar.add(btn("Thêm SP", UIUtils.COLOR_SUCCESS, e -> openProductForm(null)));
        toolbar.add(btn("Sửa", UIUtils.COLOR_SECONDARY, e -> editSelectedProduct()));
        toolbar.add(btn("Tải lại", UIUtils.COLOR_WARNING, e -> loadProducts()));
        p.add(toolbar, BorderLayout.NORTH);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UIUtils.COLOR_CARD);
        tableCard.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        applyTableStyle(tblProducts);
        tblProducts.getColumnModel().getColumn(6).setMinWidth(0);
        tblProducts.getColumnModel().getColumn(6).setMaxWidth(0);
        tblProducts.getColumnModel().getColumn(0).setPreferredWidth(100);
        tblProducts.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblProducts.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblProducts.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblProducts.getColumnModel().getColumn(4).setPreferredWidth(60);
        tblProducts.getColumnModel().getColumn(5).setPreferredWidth(80);
        JScrollPane scroll = new JScrollPane(tblProducts);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        p.add(tableCard, BorderLayout.CENTER);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        stats.setBackground(UIUtils.COLOR_CARD);
        stats.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 10, 4, 10)));
        lblProdStats.setFont(UIUtils.FONT_BOLD);
        lblProdStats.setForeground(Color.BLACK);
        stats.add(lblProdStats);
        p.add(stats, BorderLayout.SOUTH);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 2: BIẾN THỂ & GIÁ
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildVariantPriceTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIUtils.COLOR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setBackground(UIUtils.COLOR_CARD);
        top.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(8, 10, 8, 10)));
        lblSelectedProd.setFont(UIUtils.FONT_BOLD);
        lblSelectedProd.setForeground(UIUtils.COLOR_SECONDARY);
        top.add(lblSelectedProd);
        top.add(Box.createHorizontalStrut(12));
        top.add(btn("Tải biến thể SP đang chọn", UIUtils.COLOR_PRIMARY, e -> loadVariantsForSelected()));
        top.add(btn("Thêm biến thể", UIUtils.COLOR_SUCCESS, e -> openVariantForm()));
        p.add(top, BorderLayout.NORTH);

        // Left: variants table
        JPanel leftCard = new JPanel(new BorderLayout());
        leftCard.setBackground(UIUtils.COLOR_CARD);
        leftCard.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        JLabel leftTitle = new JLabel("  DANH SÁCH BIẾN THỂ");
        leftTitle.setFont(UIUtils.FONT_BOLD);
        leftTitle.setForeground(Color.BLACK);
        leftTitle.setOpaque(true);
        leftTitle.setBackground(UIUtils.COLOR_CARD);
        leftTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER));
        leftTitle.setPreferredSize(new Dimension(0, 30));
        leftCard.add(leftTitle, BorderLayout.NORTH);
        applyTableStyle(tblVariants);
        tblVariants.getColumnModel().getColumn(4).setMinWidth(0);
        tblVariants.getColumnModel().getColumn(4).setMaxWidth(0);
        JScrollPane varScroll = new JScrollPane(tblVariants);
        varScroll.setBorder(null);
        varScroll.getViewport().setBackground(Color.WHITE);
        leftCard.add(varScroll, BorderLayout.CENTER);

        // Right: price form
        JPanel rightCard = new JPanel(new BorderLayout(0, 8));
        rightCard.setBackground(UIUtils.COLOR_CARD);
        rightCard.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        JLabel rightTitle = new JLabel("  CẬP NHẬT GIÁ");
        rightTitle.setFont(UIUtils.FONT_BOLD);
        rightTitle.setForeground(Color.BLACK);
        rightTitle.setOpaque(true);
        rightTitle.setBackground(UIUtils.COLOR_CARD);
        rightTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER));
        rightTitle.setPreferredSize(new Dimension(0, 30));
        rightCard.add(rightTitle, BorderLayout.NORTH);

        JPanel priceForm = new JPanel(new GridBagLayout());
        priceForm.setBackground(UIUtils.COLOR_CARD);
        priceForm.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        JTextField txtNewPrice = new JTextField(14);
        styleTextField(txtNewPrice);
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        priceForm.add(styledLabel("Giá mới (VNĐ):"), g);
        g.gridx = 1; g.weightx = 1;
        priceForm.add(txtNewPrice, g);
        g.gridx = 0; g.gridy = 1;
        priceForm.add(styledLabel("Lưu ý:"), g);
        g.gridx = 1;
        JLabel note = new JLabel("Giá cũ sẽ được đóng, giá mới bắt đầu hiệu lực");
        note.setFont(UIUtils.FONT_SMALL);
        note.setForeground(UIUtils.COLOR_TEXT_MUTED);
        priceForm.add(note, g);
        JButton btnSetPrice = btn("Cập nhật giá", UIUtils.COLOR_SUCCESS, e -> {
            int row = tblVariants.getSelectedRow();
            if (row < 0) { UIUtils.showError(this, "Chọn biến thể để đặt giá."); return; }
            String variantId = (String) varModel.getValueAt(row, 4);
            try {
                String priceText = txtNewPrice.getText().replace(",", "").replace(".", "").trim();
                double price = Double.parseDouble(priceText);
                service.setPriceRetail(variantId, price);
                UIUtils.showSuccess(this, "Cập nhật giá thành công!");
                loadVariantsForSelected();
                txtNewPrice.setText("");
            } catch (NumberFormatException nfe) {
                UIUtils.showError(this, "Giá không hợp lệ.");
            } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        });
        btnSetPrice.setPreferredSize(new Dimension(160, 34));
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        priceForm.add(btnSetPrice, g);
        g.gridy = 3; g.weighty = 1;
        priceForm.add(Box.createVerticalGlue(), g);
        rightCard.add(priceForm, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCard, rightCard);
        split.setDividerLocation(450);
        split.setResizeWeight(0.65);
        split.setBorder(null);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 3: DANH MỤC
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildCategoryTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIUtils.COLOR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(8, 10, 8, 10)));
        toolbar.add(btn("Thêm danh mục", UIUtils.COLOR_SUCCESS, e -> openCategoryForm(null)));
        toolbar.add(btn("Tải lại", UIUtils.COLOR_PRIMARY, e -> loadCategories()));
        p.add(toolbar, BorderLayout.NORTH);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UIUtils.COLOR_CARD);
        tableCard.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        applyTableStyle(tblCats);
        tblCats.getColumnModel().getColumn(0).setPreferredWidth(150);
        tblCats.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblCats.getColumnModel().getColumn(2).setPreferredWidth(250);
        tblCats.getColumnModel().getColumn(3).setPreferredWidth(80);
        JScrollPane catScroll = new JScrollPane(tblCats);
        catScroll.setBorder(null);
        catScroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(catScroll, BorderLayout.CENTER);
        p.add(tableCard, BorderLayout.CENTER);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        stats.setBackground(UIUtils.COLOR_CARD);
        stats.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 10, 4, 10)));
        lblCatStats.setFont(UIUtils.FONT_BOLD);
        lblCatStats.setForeground(Color.BLACK);
        stats.add(lblCatStats);
        p.add(stats, BorderLayout.SOUTH);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════════════════════════
    private void loadProducts() {
        prodModel.setRowCount(0);
        int activeCount = 0, inactiveCount = 0;
        try {
            String searchText = txtProdSearch.getText().trim();
            if (searchText.equals("Mã SP, tên, thương hiệu...")) searchText = "";
            for (Product pr : service.searchProducts(searchText)) {
                prodModel.addRow(new Object[]{
                    pr.getProductCode(), pr.getName(), pr.getCategoryName(),
                    pr.getBrand(), pr.getUnit(), pr.getStatus(), pr.getProductId()
                });
                if ("ACTIVE".equalsIgnoreCase(pr.getStatus())) activeCount++;
                else inactiveCount++;
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        lblProdStats.setText(String.format("Tổng: %d sản phẩm  |  Hoạt động: %d  |  Ngưng bán: %d",
            prodModel.getRowCount(), activeCount, inactiveCount));
    }

    private void loadVariantsForSelected() {
        int row = tblProducts.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn sản phẩm từ tab 'Sản phẩm' trước."); return; }
        String productId = (String) prodModel.getValueAt(row, 6);
        String productName = (String) prodModel.getValueAt(row, 1);
        lblSelectedProd.setText("SP: " + productName);
        varModel.setRowCount(0);
        try {
            for (ProductVariant v : service.getVariants(productId)) {
                double price = service.getActivePrice(v.getVariantId());
                varModel.addRow(new Object[]{
                    v.getVariantName(), v.getBarcode(),
                    UIUtils.formatCurrency(price), v.getStatus() == 1 ? "Hoạt động" : "Tắt",
                    v.getVariantId()
                });
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void loadCategories() {
        catModel.setRowCount(0);
        try {
            for (ProductCategory c : service.getCategories()) {
                catModel.addRow(new Object[]{
                    c.getCategoryId(), c.getCategoryName(), c.getDescription(),
                    c.getStatus() == 1 ? "Hoạt động" : "Tắt"
                });
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        lblCatStats.setText("Tổng: " + catModel.getRowCount() + " danh mục");
    }

    // ═══════════════════════════════════════════════════════════════
    //  FORMS (Dialogs)
    // ═══════════════════════════════════════════════════════════════
    private void openProductForm(Product existing) {
        boolean isNew = existing == null;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            isNew ? "Thêm sản phẩm" : "Sửa sản phẩm", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(480, 360);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        JTextField txtCode  = new JTextField(existing != null ? existing.getProductCode() : "");
        JTextField txtName  = new JTextField(existing != null ? existing.getName() : "");
        JTextField txtBrand = new JTextField(existing != null ? existing.getBrand() : "");
        JTextField txtUnit  = new JTextField(existing != null ? existing.getUnit() : "pcs");
        JComboBox<String> cboCat = new JComboBox<>();
        List<ProductCategory> cats = new ArrayList<>();
        try { cats = service.getCategories(); } catch (Exception ignored) {}
        for (ProductCategory c : cats) cboCat.addItem(c.getCategoryName() + " [" + c.getCategoryId() + "]");

        String[] labels = {"Mã SP (*):", "Tên sản phẩm (*):", "Thương hiệu:", "Đơn vị:", "Danh mục:"};
        JComponent[] inputs = {txtCode, txtName, txtBrand, txtUnit, cboCat};
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0;
            form.add(styledLabel(labels[i]), gc);
            gc.gridx = 1; gc.weightx = 1;
            if (inputs[i] instanceof JTextField) styleTextField((JTextField) inputs[i]);
            form.add(inputs[i], gc);
        }
        if (!isNew) { txtCode.setEditable(false); txtCode.setBackground(new Color(240, 240, 240)); }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(Color.WHITE);
        final List<ProductCategory> finalCats = cats;
        btnPanel.add(btn("Lưu", UIUtils.COLOR_SUCCESS, e -> {
            Product pr = existing != null ? existing : new Product();
            pr.setProductCode(txtCode.getText().trim());
            pr.setName(txtName.getText().trim());
            pr.setBrand(txtBrand.getText().trim());
            pr.setUnit(txtUnit.getText().trim());
            if (cboCat.getSelectedItem() != null) {
                String sel = (String) cboCat.getSelectedItem();
                int idx = sel.lastIndexOf('[');
                if (idx >= 0) {
                    String catId = sel.substring(idx + 1, sel.length() - 1);
                    pr.setCategoryId(catId);
                }
            }
            try { service.saveProduct(pr); loadProducts(); UIUtils.showSuccess(dlg, "Lưu thành công!"); dlg.dispose(); }
            catch (Exception ex) { UIUtils.showError(dlg, ex.getMessage()); }
        }));
        btnPanel.add(btn("Hủy", UIUtils.COLOR_DANGER, e -> dlg.dispose()));

        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void editSelectedProduct() {
        int row = tblProducts.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn sản phẩm để sửa."); return; }
        try {
            Product p = new Product();
            p.setProductId((String) prodModel.getValueAt(row, 6));
            p.setProductCode((String) prodModel.getValueAt(row, 0));
            p.setName((String) prodModel.getValueAt(row, 1));
            p.setBrand((String) prodModel.getValueAt(row, 3));
            p.setUnit((String) prodModel.getValueAt(row, 4));
            openProductForm(p);
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void openCategoryForm(ProductCategory existing) {
        boolean isNew = existing == null;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            isNew ? "Thêm danh mục" : "Sửa danh mục", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 240);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        JTextField txtName = new JTextField(existing != null ? existing.getCategoryName() : "");
        JTextField txtDesc = new JTextField(existing != null ? existing.getDescription() : "");
        styleTextField(txtName); styleTextField(txtDesc);
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        form.add(styledLabel("Tên danh mục (*):"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(txtName, gc);
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        form.add(styledLabel("Mô tả:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(txtDesc, gc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btn("Lưu", UIUtils.COLOR_SUCCESS, e -> {
            ProductCategory c = existing != null ? existing : new ProductCategory();
            c.setCategoryName(txtName.getText().trim());
            c.setDescription(txtDesc.getText().trim());
            c.setStatus(1);
            try { service.saveCategory(c); loadCategories(); UIUtils.showSuccess(dlg, "Lưu thành công!"); dlg.dispose(); }
            catch (Exception ex) { UIUtils.showError(dlg, ex.getMessage()); }
        }));
        btnPanel.add(btn("Hủy", UIUtils.COLOR_DANGER, e -> dlg.dispose()));

        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void openVariantForm() {
        int row = tblProducts.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn sản phẩm từ tab 'Sản phẩm' trước khi thêm biến thể."); return; }
        String productId = (String) prodModel.getValueAt(row, 6);
        String productName = (String) prodModel.getValueAt(row, 1);

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Thêm biến thể cho: " + productName, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 260);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtVarName = new JTextField();
        JTextField txtBarcode = new JTextField();
        JTextField txtPrice   = new JTextField();
        styleTextField(txtVarName); styleTextField(txtBarcode); styleTextField(txtPrice);

        String[] labels = {"Tên biến thể (*):", "Barcode:", "Giá bán (VNĐ):"};
        JTextField[] fields = {txtVarName, txtBarcode, txtPrice};
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0;
            form.add(styledLabel(labels[i]), gc);
            gc.gridx = 1; gc.weightx = 1;
            form.add(fields[i], gc);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btn("Lưu", UIUtils.COLOR_SUCCESS, e -> {
            String varName = txtVarName.getText().trim();
            if (varName.isEmpty()) { UIUtils.showError(dlg, "Tên biến thể bắt buộc."); return; }
            try {
                ProductVariant v = new ProductVariant();
                v.setProductId(productId);
                v.setVariantName(varName);
                v.setBarcode(txtBarcode.getText().trim());
                v.setStatus(1);
                service.saveVariant(v);
                String priceText = txtPrice.getText().replace(",", "").replace(".", "").trim();
                if (!priceText.isEmpty()) {
                    double price = Double.parseDouble(priceText);
                    service.setPriceRetail(v.getVariantId(), price);
                }
                loadVariantsForSelected();
                UIUtils.showSuccess(dlg, "Thêm biến thể thành công!");
                dlg.dispose();
            } catch (NumberFormatException nfe) {
                UIUtils.showError(dlg, "Giá không hợp lệ.");
            } catch (Exception ex) { UIUtils.showError(dlg, ex.getMessage()); }
        }));
        btnPanel.add(btn("Hủy", UIUtils.COLOR_DANGER, e -> dlg.dispose()));

        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════
    //  UI COMPONENT HELPERS
    // ═══════════════════════════════════════════════════════════════
    private void applyTableStyle(JTable table) {
        table.setFont(UIUtils.FONT_LABEL);
        table.setForeground(Color.BLACK);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(Color.BLACK);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setFont(UIUtils.FONT_LABEL);
                setForeground(Color.BLACK);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : UIUtils.COLOR_TABLE_EVEN);
                return this;
            }
        });
        JTableHeader hdr = table.getTableHeader();
        hdr.setFont(UIUtils.FONT_BOLD);
        hdr.setBackground(UIUtils.COLOR_CARD);
        hdr.setForeground(Color.BLACK);
        hdr.setReorderingAllowed(false);
        hdr.setPreferredSize(new Dimension(0, 32));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.COLOR_BORDER));
    }

    private JButton btn(String text, Color bg, java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(UIUtils.FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int textWidth = b.getFontMetrics(b.getFont()).stringWidth(text);
        b.setPreferredSize(new Dimension(textWidth + 36, 32));
        b.setMinimumSize(new Dimension(textWidth + 36, 32));
        if (action != null) b.addActionListener(action);
        return b;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIUtils.FONT_BOLD);
        l.setForeground(Color.BLACK);
        return l;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(UIUtils.FONT_LABEL);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(4, 8, 4, 8)));
    }

    private JSeparator vertSep() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(UIUtils.COLOR_BORDER);
        return sep;
    }

    /** Add a greyed-out placeholder text that disappears on focus. */
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
