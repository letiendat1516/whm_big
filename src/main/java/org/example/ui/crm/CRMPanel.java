package org.example.ui.crm;

import org.example.model.Customer;
import org.example.service.CRMService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Module 4 — CRM & Promotion Panel (Khách hàng & Khuyến mãi)
 * Tabs: Khách hàng | Điểm thưởng | Khuyến mãi
 */
public class CRMPanel extends JPanel {

    private final CRMService crmService = new CRMService();

    // Customer table
    private final DefaultTableModel cusModel = new DefaultTableModel(
            new String[]{"Họ tên", "Điện thoại", "Email", "Ngày đăng ký", "Hạng", "Điểm", "CustomerID"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblCustomers = new JTable(cusModel);
    private final JTextField txtSearch = new JTextField(20);

    // Points history
    private final DefaultTableModel ptsModel = new DefaultTableModel(
            new String[]{"Loại", "Điểm", "Thời gian", "Ghi chú", "Mã đơn"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblPoints = new JTable(ptsModel);

    public CRMPanel() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.COLOR_BG);

        JLabel title = UIUtils.sectionLabel("  KHÁCH HÀNG & KHUYẾN MÃI (CRM)");
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
        tabs.addTab("Khách hàng", buildCustomersTab());
        tabs.addTab("Điểm thưởng", buildPointsTab());
        tabs.addTab("Khuyến mãi", buildPromotionTab());
        add(tabs, BorderLayout.CENTER);

        loadCustomers();
    }

    private JPanel buildCustomersTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        JLabel lblSearch = new JLabel("Tìm:");
        lblSearch.setFont(UIUtils.FONT_BOLD);
        lblSearch.setForeground(Color.BLACK);
        toolbar.add(lblSearch);
        txtSearch.setToolTipText("Nhập tên, SĐT hoặc email khách hàng");
        addPlaceholder(txtSearch, "Tên, SĐT hoặc email...");
        toolbar.add(txtSearch);
        JButton btnSearch = UIUtils.primaryButton("Tìm");
        btnSearch.addActionListener(e -> loadCustomers());
        txtSearch.addActionListener(e -> loadCustomers());
        JButton btnAdd = UIUtils.successButton("Thêm KH");
        btnAdd.addActionListener(e -> openCustomerForm(null));
        JButton btnEdit = UIUtils.secondaryButton("Sửa");
        btnEdit.addActionListener(e -> editSelectedCustomer());
        toolbar.add(btnSearch); toolbar.add(btnAdd); toolbar.add(btnEdit);
        p.add(toolbar, BorderLayout.NORTH);

        UIUtils.applyZebraRenderer(tblCustomers);
        // Hide CustomerID column completely
        tblCustomers.getColumnModel().getColumn(6).setMinWidth(0);
        tblCustomers.getColumnModel().getColumn(6).setMaxWidth(0);
        tblCustomers.getColumnModel().getColumn(6).setWidth(0);
        tblCustomers.getColumnModel().getColumn(6).setPreferredWidth(0);
        tblCustomers.getColumnModel().getColumn(0).setPreferredWidth(160);
        tblCustomers.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblCustomers.getColumnModel().getColumn(2).setPreferredWidth(160);
        tblCustomers.getColumnModel().getColumn(3).setPreferredWidth(110);
        tblCustomers.getColumnModel().getColumn(4).setPreferredWidth(80);
        tblCustomers.getColumnModel().getColumn(5).setPreferredWidth(70);
        tblCustomers.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p.add(new JScrollPane(tblCustomers), BorderLayout.CENTER);

        // Stats bar
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stats.setBackground(new Color(237, 231, 246));
        JLabel lblHint = new JLabel("Chọn khách hàng và chuyển tab 'Điểm thưởng' để xem lịch sử.");
        lblHint.setFont(UIUtils.FONT_LABEL);
        lblHint.setForeground(Color.BLACK);
        stats.add(lblHint);
        p.add(stats, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildPointsTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setBackground(UIUtils.COLOR_CARD);
        top.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        JLabel lblPtsHint = new JLabel("Xem lịch sử điểm của khách đang chọn:");
        lblPtsHint.setFont(UIUtils.FONT_BOLD);
        lblPtsHint.setForeground(Color.BLACK);
        top.add(lblPtsHint);
        JButton btnLoad = UIUtils.primaryButton("Tải lịch sử điểm");
        btnLoad.addActionListener(e -> loadPointHistory());
        top.add(btnLoad);
        p.add(top, BorderLayout.NORTH);

        UIUtils.applyZebraRenderer(tblPoints);
        tblPoints.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblPoints.getColumnModel().getColumn(1).setPreferredWidth(60);
        p.add(new JScrollPane(tblPoints), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildPromotionTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Split: Campaigns top, Promotions/Vouchers bottom
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.35);

        // ── CAMPAIGN section ──
        JPanel campaignPanel = new JPanel(new BorderLayout(4, 4));
        campaignPanel.setBackground(UIUtils.COLOR_CARD);

        DefaultTableModel campModel = new DefaultTableModel(
                new String[]{"Tên chiến dịch", "Mô tả", "Bắt đầu", "Kết thúc", "Hoạt động", "CampaignID"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblCamp = new JTable(campModel);
        UIUtils.applyZebraRenderer(tblCamp);
        tblCamp.getColumnModel().getColumn(5).setMinWidth(0);
        tblCamp.getColumnModel().getColumn(5).setMaxWidth(0);
        tblCamp.getColumnModel().getColumn(5).setWidth(0);
        tblCamp.getColumnModel().getColumn(5).setPreferredWidth(0);

        JPanel campToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        campToolbar.setBackground(UIUtils.COLOR_CARD);
        campToolbar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        JLabel lblCamp = new JLabel("Chiến dịch KM:");
        lblCamp.setFont(UIUtils.FONT_BOLD); lblCamp.setForeground(Color.BLACK);
        campToolbar.add(lblCamp);

        JButton btnLoadCamp = UIUtils.primaryButton("Tải DS");
        JButton btnAddCamp = UIUtils.successButton("Thêm chiến dịch");
        JButton btnEditCamp = UIUtils.secondaryButton("Sửa");
        JButton btnDelCamp = UIUtils.dangerButton("Xóa");
        campToolbar.add(btnLoadCamp); campToolbar.add(btnAddCamp);
        campToolbar.add(btnEditCamp); campToolbar.add(btnDelCamp);

        campaignPanel.add(campToolbar, BorderLayout.NORTH);
        campaignPanel.add(new JScrollPane(tblCamp), BorderLayout.CENTER);
        split.setTopComponent(campaignPanel);

        // ── PROMOTION / VOUCHER section ──
        JPanel promoPanel = new JPanel(new BorderLayout(4, 4));
        promoPanel.setBackground(UIUtils.COLOR_CARD);

        DefaultTableModel promoModel = new DefaultTableModel(
                new String[]{"Tên KM", "Loại", "Ưu tiên", "Voucher Code", "Giá trị", "Đã dùng", "Tối đa", "Hết hạn", "Kích hoạt", "PromotionID", "CampaignID"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblPromo = new JTable(promoModel);
        UIUtils.applyZebraRenderer(tblPromo);
        tblPromo.getColumnModel().getColumn(9).setMinWidth(0);
        tblPromo.getColumnModel().getColumn(9).setMaxWidth(0);
        tblPromo.getColumnModel().getColumn(9).setWidth(0);
        tblPromo.getColumnModel().getColumn(9).setPreferredWidth(0);
        tblPromo.getColumnModel().getColumn(10).setMinWidth(0);
        tblPromo.getColumnModel().getColumn(10).setMaxWidth(0);
        tblPromo.getColumnModel().getColumn(10).setWidth(0);
        tblPromo.getColumnModel().getColumn(10).setPreferredWidth(0);

        JPanel promoToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        promoToolbar.setBackground(UIUtils.COLOR_CARD);
        promoToolbar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        JLabel lblPromo = new JLabel("Khuyến mãi / Voucher:");
        lblPromo.setFont(UIUtils.FONT_BOLD); lblPromo.setForeground(Color.BLACK);
        promoToolbar.add(lblPromo);

        JButton btnLoadPromo = UIUtils.primaryButton("Tải DS");
        JButton btnAddPromo = UIUtils.successButton("Thêm KM");
        JButton btnAddVoucher = UIUtils.successButton("Tạo Voucher");
        JButton btnEditPromo = UIUtils.secondaryButton("Sửa");
        JButton btnDelPromo = UIUtils.dangerButton("Xóa");
        promoToolbar.add(btnLoadPromo); promoToolbar.add(btnAddPromo);
        promoToolbar.add(btnAddVoucher); promoToolbar.add(btnEditPromo);
        promoToolbar.add(btnDelPromo);

        promoPanel.add(promoToolbar, BorderLayout.NORTH);
        promoPanel.add(new JScrollPane(tblPromo), BorderLayout.CENTER);
        split.setBottomComponent(promoPanel);

        p.add(split, BorderLayout.CENTER);

        // ── DAO reference ──
        org.example.dao.OrderDAO dao = new org.example.dao.OrderDAO();
        org.example.dao.PromotionDAO promoDAO = new org.example.dao.PromotionDAO();

        // ── Campaign data loaders ──
        Runnable loadCampaigns = () -> {
            campModel.setRowCount(0);
            try {
                for (org.example.model.Campaign c : dao.findAllCampaigns()) {
                    campModel.addRow(new Object[]{c.getName(), c.getDescription(),
                        c.getStartDate(), c.getEndDate(), c.getIsActive() == 1 ? "✔" : "✖", c.getCampaignId()});
                }
            } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        };

        Runnable loadPromotions = () -> {
            promoModel.setRowCount(0);
            try {
                for (org.example.model.Promotion pr : promoDAO.findAll()) {
                    promoModel.addRow(new Object[]{pr.getName(), translatePromoType(pr.getPromoType()),
                        pr.getPriority(), pr.getVoucherCode(), pr.getRuleDefinition(),
                        pr.getCurrentUsageCount(), pr.getMaxUsageCount(), pr.getExpiryDate(),
                        pr.getIsActive() == 1 ? "✔" : "✖", pr.getPromotionId(), pr.getCampaignId()});
                }
            } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        };

        // ── Campaign actions ──
        btnLoadCamp.addActionListener(e -> loadCampaigns.run());
        btnAddCamp.addActionListener(e -> openCampaignForm(null, dao, loadCampaigns));
        btnEditCamp.addActionListener(e -> {
            int row = tblCamp.getSelectedRow();
            if (row < 0) { UIUtils.showError(this, "Chọn chiến dịch."); return; }
            org.example.model.Campaign c = new org.example.model.Campaign();
            c.setCampaignId((String) campModel.getValueAt(row, 5));
            c.setName((String) campModel.getValueAt(row, 0));
            c.setDescription((String) campModel.getValueAt(row, 1));
            c.setStartDate((String) campModel.getValueAt(row, 2));
            c.setEndDate((String) campModel.getValueAt(row, 3));
            c.setIsActive("✔".equals(campModel.getValueAt(row, 4)) ? 1 : 0);
            openCampaignForm(c, dao, loadCampaigns);
        });
        btnDelCamp.addActionListener(e -> {
            int row = tblCamp.getSelectedRow();
            if (row < 0) { UIUtils.showError(this, "Chọn chiến dịch."); return; }
            if (JOptionPane.showConfirmDialog(this, "Xóa chiến dịch này?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try { dao.deleteCampaign((String) campModel.getValueAt(row, 5)); loadCampaigns.run();
                  UIUtils.showSuccess(this, "Đã xóa."); }
            catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        });

        // ── Promotion actions ──
        btnLoadPromo.addActionListener(e -> loadPromotions.run());
        btnAddPromo.addActionListener(e -> openPromotionForm(null, dao, loadPromotions));
        btnAddVoucher.addActionListener(e -> openVoucherForm(dao, loadPromotions));
        btnEditPromo.addActionListener(e -> {
            int row = tblPromo.getSelectedRow();
            if (row < 0) { UIUtils.showError(this, "Chọn khuyến mãi."); return; }
            org.example.model.Promotion pr = new org.example.model.Promotion();
            pr.setPromotionId((String) promoModel.getValueAt(row, 9));
            pr.setCampaignId((String) promoModel.getValueAt(row, 10));
            pr.setName((String) promoModel.getValueAt(row, 0));
            pr.setPromoType(reversePromoType((String) promoModel.getValueAt(row, 1)));
            pr.setPriority((Integer) promoModel.getValueAt(row, 2));
            pr.setVoucherCode((String) promoModel.getValueAt(row, 3));
            pr.setRuleDefinition((String) promoModel.getValueAt(row, 4));
            pr.setCurrentUsageCount((Integer) promoModel.getValueAt(row, 5));
            pr.setMaxUsageCount((Integer) promoModel.getValueAt(row, 6));
            pr.setExpiryDate((String) promoModel.getValueAt(row, 7));
            pr.setIsActive("✔".equals(promoModel.getValueAt(row, 8)) ? 1 : 0);
            openPromotionForm(pr, dao, loadPromotions);
        });
        btnDelPromo.addActionListener(e -> {
            int row = tblPromo.getSelectedRow();
            if (row < 0) { UIUtils.showError(this, "Chọn khuyến mãi."); return; }
            if (JOptionPane.showConfirmDialog(this, "Xóa khuyến mãi này?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try { dao.deletePromotion((String) promoModel.getValueAt(row, 9)); loadPromotions.run();
                  UIUtils.showSuccess(this, "Đã xóa."); }
            catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
        });

        // Initial load
        loadCampaigns.run();
        loadPromotions.run();

        return p;
    }

    // ── Data ───────────────────────────────────────────────────────
    private void loadCustomers() {
        cusModel.setRowCount(0);
        try {
            String searchText = txtSearch.getText().trim();
            // Ignore placeholder text
            if (searchText.equals("Tên, SĐT hoặc email...")) searchText = "";
            List<Customer> list = crmService.searchCustomers(searchText);
            for (Customer c : list) {
                cusModel.addRow(new Object[]{
                    c.getFullName(), c.getPhoneNum(), c.getEmail(),
                    c.getRegistrationDate(), c.getTierName(), c.getCurrentPoints(),
                    c.getCustomerId()
                });
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void loadPointHistory() {
        int row = tblCustomers.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn khách hàng từ tab Khách hàng."); return; }
        String customerId = (String) cusModel.getValueAt(row, 6);
        ptsModel.setRowCount(0);
        try {
            for (Object[] r : crmService.getPointHistory(customerId)) {
                ptsModel.addRow(r);
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void openCustomerForm(Customer existing) {
        JTextField txtName  = new JTextField(existing != null ? existing.getFullName() : "");
        JTextField txtPhone = new JTextField(existing != null ? existing.getPhoneNum() : "");
        JTextField txtEmail = new JTextField(existing != null ? existing.getEmail() : "");
        Object[] fields = {"Họ tên (*):", txtName, "Điện thoại (*):", txtPhone, "Email:", txtEmail};
        int res = JOptionPane.showConfirmDialog(this, fields,
                existing == null ? "Thêm khách hàng" : "Sửa khách hàng", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        Customer c = existing != null ? existing : new Customer();
        c.setFullName(txtName.getText().trim());
        c.setPhoneNum(txtPhone.getText().trim());
        c.setEmail(txtEmail.getText().trim());
        try { crmService.saveCustomer(c); loadCustomers(); UIUtils.showSuccess(this, "Lưu thành công!"); }
        catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void editSelectedCustomer() {
        int row = tblCustomers.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn khách hàng để sửa."); return; }
        Customer c = new Customer();
        c.setCustomerId((String) cusModel.getValueAt(row, 6));
        c.setFullName((String) cusModel.getValueAt(row, 0));
        c.setPhoneNum((String) cusModel.getValueAt(row, 1));
        c.setEmail((String) cusModel.getValueAt(row, 2));
        openCustomerForm(c);
    }

    // ── Campaign Form ──
    private void openCampaignForm(org.example.model.Campaign existing,
                                   org.example.dao.OrderDAO dao, Runnable reload) {
        JTextField txtName = new JTextField(existing != null ? existing.getName() : "");
        JTextField txtDesc = new JTextField(existing != null ? existing.getDescription() : "");
        JTextField txtStart = new JTextField(existing != null && existing.getStartDate() != null ?
                existing.getStartDate() : java.time.LocalDate.now().toString());
        JTextField txtEnd = new JTextField(existing != null && existing.getEndDate() != null ?
                existing.getEndDate() : java.time.LocalDate.now().plusMonths(1).toString());
        JCheckBox chkActive = new JCheckBox("Kích hoạt", existing == null || existing.getIsActive() == 1);
        Object[] fields = {"Tên chiến dịch (*):", txtName, "Mô tả:", txtDesc,
                "Ngày bắt đầu (YYYY-MM-DD):", txtStart, "Ngày kết thúc (YYYY-MM-DD):", txtEnd, chkActive};
        int res = JOptionPane.showConfirmDialog(this, fields,
                existing == null ? "Thêm chiến dịch" : "Sửa chiến dịch", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        if (txtName.getText().trim().isEmpty()) { UIUtils.showError(this, "Tên chiến dịch bắt buộc."); return; }
        org.example.model.Campaign c = existing != null ? existing : new org.example.model.Campaign();
        c.setName(txtName.getText().trim()); c.setDescription(txtDesc.getText().trim());
        c.setStartDate(txtStart.getText().trim()); c.setEndDate(txtEnd.getText().trim());
        c.setIsActive(chkActive.isSelected() ? 1 : 0);
        try { dao.saveCampaign(c); reload.run(); UIUtils.showSuccess(this, "Lưu thành công!"); }
        catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    // ── Promotion Form ──
    private void openPromotionForm(org.example.model.Promotion existing,
                                    org.example.dao.OrderDAO dao, Runnable reload) {
        // Get campaigns for combo
        java.util.List<org.example.model.Campaign> campaigns;
        try { campaigns = dao.findAllCampaigns(); }
        catch (Exception ex) { UIUtils.showError(this, "Lỗi: " + ex.getMessage()); return; }
        if (campaigns.isEmpty()) { UIUtils.showError(this, "Tạo chiến dịch trước."); return; }

        JComboBox<String> cboCamp = new JComboBox<>();
        java.util.Map<String, String> campMap = new java.util.LinkedHashMap<>();
        for (org.example.model.Campaign c : campaigns) {
            String display = c.getName() + " (" + c.getStartDate() + " → " + c.getEndDate() + ")";
            cboCamp.addItem(display); campMap.put(display, c.getCampaignId());
        }
        if (existing != null) {
            for (int i = 0; i < cboCamp.getItemCount(); i++) {
                if (campMap.get(cboCamp.getItemAt(i)).equals(existing.getCampaignId())) {
                    cboCamp.setSelectedIndex(i); break;
                }
            }
        }

        JTextField txtName = new JTextField(existing != null ? existing.getName() : "");
        JComboBox<String> cboType = new JComboBox<>(new String[]{"PERCENT_DISCOUNT", "FIXED_DISCOUNT", "BUY_X_GET_Y", "VOUCHER", "POINTS_MULTIPLIER"});
        if (existing != null) cboType.setSelectedItem(existing.getPromoType());
        JTextField txtRule = new JTextField(existing != null ? existing.getRuleDefinition() : "10");
        JTextField txtPriority = new JTextField(existing != null ? String.valueOf(existing.getPriority()) : "1");
        JTextField txtVoucher = new JTextField(existing != null ? existing.getVoucherCode() : "");
        JTextField txtMax = new JTextField(existing != null ? String.valueOf(existing.getMaxUsageCount()) : "0");
        JTextField txtExpiry = new JTextField(existing != null && existing.getExpiryDate() != null ?
                existing.getExpiryDate() : "");
        JTextField txtTrigger = new JTextField(existing != null ? existing.getTriggerCondition() : "");
        JCheckBox chkActive = new JCheckBox("Kích hoạt", existing == null || existing.getIsActive() == 1);

        Object[] fields = {"Chiến dịch:", cboCamp, "Tên khuyến mãi (*):", txtName,
                "Loại:", cboType, "Giá trị (%, số tiền):", txtRule,
                "Ưu tiên:", txtPriority, "Mã voucher:", txtVoucher,
                "Số lần sử dụng tối đa (0=vô hạn):", txtMax,
                "Hết hạn (YYYY-MM-DD):", txtExpiry, "Điều kiện:", txtTrigger, chkActive};
        int res = JOptionPane.showConfirmDialog(this, fields,
                existing == null ? "Thêm khuyến mãi" : "Sửa khuyến mãi", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        if (txtName.getText().trim().isEmpty()) { UIUtils.showError(this, "Tên khuyến mãi bắt buộc."); return; }

        org.example.model.Promotion pr = existing != null ? existing : new org.example.model.Promotion();
        pr.setCampaignId(campMap.get(cboCamp.getSelectedItem()));
        pr.setName(txtName.getText().trim());
        pr.setPromoType((String) cboType.getSelectedItem());
        pr.setRuleDefinition(txtRule.getText().trim());
        try { pr.setPriority(Integer.parseInt(txtPriority.getText().trim())); } catch (NumberFormatException e) { pr.setPriority(1); }
        pr.setVoucherCode(txtVoucher.getText().trim().isEmpty() ? null : txtVoucher.getText().trim());
        try { pr.setMaxUsageCount(Integer.parseInt(txtMax.getText().trim())); } catch (NumberFormatException e) { pr.setMaxUsageCount(0); }
        pr.setExpiryDate(txtExpiry.getText().trim().isEmpty() ? null : txtExpiry.getText().trim());
        pr.setTriggerCondition(txtTrigger.getText().trim().isEmpty() ? null : txtTrigger.getText().trim());
        pr.setIsActive(chkActive.isSelected() ? 1 : 0);
        try { dao.savePromotion(pr); reload.run(); UIUtils.showSuccess(this, "Lưu thành công!"); }
        catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    // ── Quick Voucher Creator ──
    private void openVoucherForm(org.example.dao.OrderDAO dao, Runnable reload) {
        java.util.List<org.example.model.Campaign> campaigns;
        try { campaigns = dao.findAllCampaigns(); }
        catch (Exception ex) { UIUtils.showError(this, "Lỗi: " + ex.getMessage()); return; }
        if (campaigns.isEmpty()) { UIUtils.showError(this, "Tạo chiến dịch trước."); return; }

        JComboBox<String> cboCamp = new JComboBox<>();
        java.util.Map<String, String> campMap = new java.util.LinkedHashMap<>();
        for (org.example.model.Campaign c : campaigns) {
            String display = c.getName(); cboCamp.addItem(display); campMap.put(display, c.getCampaignId());
        }

        JTextField txtName = new JTextField("Voucher giảm giá");
        JComboBox<String> cboType = new JComboBox<>(new String[]{"PERCENT_DISCOUNT", "FIXED_DISCOUNT"});
        JTextField txtValue = new JTextField("10");
        JTextField txtCode = new JTextField(generateVoucherCode());
        JTextField txtMax = new JTextField("100");
        JTextField txtExpiry = new JTextField(java.time.LocalDate.now().plusMonths(3).toString());

        Object[] fields = {"Chiến dịch:", cboCamp, "Tên voucher:", txtName,
                "Loại giảm:", cboType, "Giá trị (% hoặc VNĐ):", txtValue,
                "Mã voucher:", txtCode, "Giới hạn sử dụng:", txtMax,
                "Hết hạn (YYYY-MM-DD):", txtExpiry};
        int res = JOptionPane.showConfirmDialog(this, fields, "Tạo Voucher nhanh", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        org.example.model.Promotion pr = new org.example.model.Promotion();
        pr.setCampaignId(campMap.get(cboCamp.getSelectedItem()));
        pr.setName(txtName.getText().trim());
        pr.setPromoType((String) cboType.getSelectedItem());
        pr.setRuleDefinition(txtValue.getText().trim());
        pr.setVoucherCode(txtCode.getText().trim().toUpperCase());
        pr.setPriority(5);
        try { pr.setMaxUsageCount(Integer.parseInt(txtMax.getText().trim())); } catch (NumberFormatException e) { pr.setMaxUsageCount(100); }
        pr.setExpiryDate(txtExpiry.getText().trim());
        pr.setIsActive(1);
        try { dao.savePromotion(pr); reload.run();
              UIUtils.showSuccess(this, "Tạo voucher: " + pr.getVoucherCode()); }
        catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private String generateVoucherCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("VLXD-");
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private String translatePromoType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "PERCENT_DISCOUNT" -> "Giảm %";
            case "FIXED_DISCOUNT" -> "Giảm tiền";
            case "BUY_X_GET_Y" -> "Mua X tặng Y";
            case "VOUCHER" -> "Voucher";
            case "POINTS_MULTIPLIER" -> "Nhân điểm";
            default -> type;
        };
    }

    private String reversePromoType(String display) {
        if (display == null) return "";
        return switch (display) {
            case "Giảm %" -> "PERCENT_DISCOUNT";
            case "Giảm tiền" -> "FIXED_DISCOUNT";
            case "Mua X tặng Y" -> "BUY_X_GET_Y";
            case "Voucher" -> "VOUCHER";
            case "Nhân điểm" -> "POINTS_MULTIPLIER";
            default -> display;
        };
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

