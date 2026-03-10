package org.example.ui.pos;

import org.example.model.Product;
import org.example.model.ProductVariant;
import org.example.service.POSService;
import org.example.service.ProductService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Floating product search dialog used by POS. */
public class ProductSearchDialog extends JDialog {

    private final ProductService productService = new ProductService();
    private final POSService posService;
    private boolean confirmed = false;

    private final JTextField txtSearch = new JTextField(20);
    private final DefaultTableModel variantModel = new DefaultTableModel(
            new String[]{"Sản phẩm", "Biến thể", "Barcode", "Giá", "variant_id"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblVariants = new JTable(variantModel);
    private final JSpinner spinQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));

    public ProductSearchDialog(JFrame parent, POSService posService) {
        super(parent, "Tìm sản phẩm", true);
        this.posService = posService;
        setSize(640, 450);
        setLocationRelativeTo(parent);
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Search bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Tìm:"));
        txtSearch.setToolTipText("Nhập tên sản phẩm, mã SP hoặc barcode");
        addPlaceholder(txtSearch, "Tên SP, mã SP, barcode...");
        top.add(txtSearch);
        JButton btnSearch = UIUtils.primaryButton("Tìm kiếm");
        top.add(btnSearch);
        main.add(top, BorderLayout.NORTH);

        // Table
        UIUtils.applyZebraRenderer(tblVariants);
        tblVariants.getColumnModel().getColumn(4).setMaxWidth(0);
        tblVariants.getColumnModel().getColumn(4).setMinWidth(0);
        main.add(new JScrollPane(tblVariants), BorderLayout.CENTER);

        // Bottom
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bot.add(new JLabel("Số lượng:"));
        bot.add(spinQty);
        JButton btnAdd = UIUtils.successButton("Thêm vào giỏ");
        JButton btnClose = UIUtils.dangerButton("Đóng");
        bot.add(btnAdd);
        bot.add(btnClose);
        main.add(bot, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> doSearch());
        btnAdd.addActionListener(e -> doAdd());
        btnClose.addActionListener(e -> dispose());
        txtSearch.addActionListener(e -> doSearch());

        setContentPane(main);
    }

    private void doSearch() {
        String q = txtSearch.getText().trim();
        if (q.equals("Tên SP, mã SP, barcode...")) q = "";
        variantModel.setRowCount(0);
        try {
            List<Product> products = productService.searchProducts(q);
            for (Product p : products) {
                List<ProductVariant> variants = productService.getVariants(p.getProductId());
                for (ProductVariant v : variants) {
                    double price = productService.getActivePrice(v.getVariantId());
                    variantModel.addRow(new Object[]{
                        p.getName(), v.getVariantName(), v.getBarcode(),
                        UIUtils.formatCurrency(price), v.getVariantId()
                    });
                }
            }
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void doAdd() {
        int row = tblVariants.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn sản phẩm trước."); return; }
        String variantId = (String) variantModel.getValueAt(row, 4);
        int qty = (int) spinQty.getValue();
        try {
            posService.addVariantToCart(variantId, qty);
            confirmed = true;
            UIUtils.showSuccess(this, "Đã thêm vào giỏ hàng.");
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    public boolean isConfirmed() { return confirmed; }

    /** Add a greyed-out placeholder text that disappears on focus. */
    private void addPlaceholder(javax.swing.JTextField field, String placeholder) {
        field.setForeground(java.awt.Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(java.awt.Color.BLACK);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(java.awt.Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }
}

