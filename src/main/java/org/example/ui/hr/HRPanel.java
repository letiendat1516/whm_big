package org.example.ui.hr;

import org.example.model.*;
import org.example.service.HRService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Module 5 — HR & Shift Panel (Nhân sự & Ca làm việc)
 * Tabs: Nhân viên | Lịch ca | Chấm công
 */
public class HRPanel extends JPanel {

    private final HRService hrService = new HRService();
    private static final String STORE_ID = "STORE-001";

    // Employee table
    private final DefaultTableModel empModel = new DefaultTableModel(
            new String[]{"Mã NV", "Họ tên", "Ngày vào", "Lương cơ bản", "Trạng thái", "employeeId"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblEmp = new JTable(empModel);

    // Shift schedule table
    private final DefaultTableModel schedModel = new DefaultTableModel(
            new String[]{"Nhân viên", "Ca", "Bắt đầu", "Kết thúc", "Trạng thái", "assignmentId"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblSched = new JTable(schedModel);
    private final JSpinner spinDate = new JSpinner(new SpinnerDateModel());

    // Attendance table
    private final DefaultTableModel attModel = new DefaultTableModel(
            new String[]{"Nhân viên", "Ca", "Giờ vào", "Giờ ra", "Ngày", "Trạng thái", "attendanceId", "assignmentId"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblAtt = new JTable(attModel);

    public HRPanel() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.COLOR_BG);

        JLabel title = UIUtils.sectionLabel("  NHÂN SỰ & CA LÀM VIỆC (HR)");
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
        tabs.addTab("Nhân viên", buildEmployeeTab());
        tabs.addTab("Lịch ca", buildScheduleTab());
        tabs.addTab("Chấm công", buildAttendanceTab());
        add(tabs, BorderLayout.CENTER);

        loadEmployees();
        loadSchedule();
        loadAttendance();
    }

    // ── Employee Tab ───────────────────────────────────────────────
    private JPanel buildEmployeeTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        JButton btnAdd  = UIUtils.successButton("Thêm NV");
        JButton btnEdit = UIUtils.secondaryButton("Sửa");
        JButton btnRefresh = UIUtils.primaryButton("Làm mới");
        btnAdd.addActionListener(e -> openEmployeeForm(null));
        btnEdit.addActionListener(e -> editSelectedEmployee());
        btnRefresh.addActionListener(e -> loadEmployees());
        toolbar.add(btnAdd); toolbar.add(btnEdit); toolbar.add(btnRefresh);
        p.add(toolbar, BorderLayout.NORTH);

        UIUtils.applyZebraRenderer(tblEmp);
        // Hide employeeId column completely
        tblEmp.getColumnModel().getColumn(5).setMinWidth(0);
        tblEmp.getColumnModel().getColumn(5).setMaxWidth(0);
        tblEmp.getColumnModel().getColumn(5).setWidth(0);
        tblEmp.getColumnModel().getColumn(5).setPreferredWidth(0);
        tblEmp.getColumnModel().getColumn(0).setPreferredWidth(100);
        tblEmp.getColumnModel().getColumn(1).setPreferredWidth(180);
        tblEmp.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblEmp.getColumnModel().getColumn(3).setPreferredWidth(130);
        tblEmp.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblEmp.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p.add(new JScrollPane(tblEmp), BorderLayout.CENTER);
        return p;
    }

    // ── Schedule Tab ───────────────────────────────────────────────
    private JPanel buildScheduleTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spinDate, "dd/MM/yyyy");
        spinDate.setEditor(dateEditor);
        JLabel lblDate = new JLabel("Ngày:");
        lblDate.setFont(UIUtils.FONT_BOLD);
        lblDate.setForeground(Color.BLACK);
        toolbar.add(lblDate);
        toolbar.add(spinDate);
        JButton btnLoad = UIUtils.primaryButton("Xem lịch");
        btnLoad.addActionListener(e -> loadSchedule());
        JButton btnAssign = UIUtils.successButton("Phân ca");
        btnAssign.addActionListener(e -> openAssignShiftDialog());
        toolbar.add(btnLoad); toolbar.add(btnAssign);
        p.add(toolbar, BorderLayout.NORTH);

        UIUtils.applyZebraRenderer(tblSched);
        // Hide assignmentId column completely
        tblSched.getColumnModel().getColumn(5).setMinWidth(0);
        tblSched.getColumnModel().getColumn(5).setMaxWidth(0);
        tblSched.getColumnModel().getColumn(5).setWidth(0);
        tblSched.getColumnModel().getColumn(5).setPreferredWidth(0);
        tblSched.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p.add(new JScrollPane(tblSched), BorderLayout.CENTER);
        return p;
    }

    // ── Attendance Tab ─────────────────────────────────────────────
    private JPanel buildAttendanceTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(UIUtils.COLOR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UIUtils.COLOR_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        JButton btnRefresh = UIUtils.primaryButton("Làm mới");
        JButton btnIn = UIUtils.successButton("Chấm vào");
        JButton btnOut = UIUtils.secondaryButton("Chấm ra");
        btnRefresh.addActionListener(e -> loadAttendance());
        btnIn.addActionListener(e -> clockIn());
        btnOut.addActionListener(e -> clockOut());
        toolbar.add(btnRefresh); toolbar.add(btnIn); toolbar.add(btnOut);
        JLabel lblAttHint = new JLabel("  * Chọn dòng trên bảng lịch ca rồi bấm chấm công");
        lblAttHint.setFont(UIUtils.FONT_SMALL);
        lblAttHint.setForeground(Color.BLACK);
        toolbar.add(lblAttHint);
        p.add(toolbar, BorderLayout.NORTH);

        UIUtils.applyZebraRenderer(tblAtt);
        // Hide attendanceId and assignmentId columns completely
        tblAtt.getColumnModel().getColumn(6).setMinWidth(0);
        tblAtt.getColumnModel().getColumn(6).setMaxWidth(0);
        tblAtt.getColumnModel().getColumn(6).setWidth(0);
        tblAtt.getColumnModel().getColumn(6).setPreferredWidth(0);
        tblAtt.getColumnModel().getColumn(7).setMinWidth(0);
        tblAtt.getColumnModel().getColumn(7).setMaxWidth(0);
        tblAtt.getColumnModel().getColumn(7).setWidth(0);
        tblAtt.getColumnModel().getColumn(7).setPreferredWidth(0);
        tblAtt.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p.add(new JScrollPane(tblAtt), BorderLayout.CENTER);
        return p;
    }

    // ── Data ───────────────────────────────────────────────────────
    private void loadEmployees() {
        empModel.setRowCount(0);
        try {
            for (Employee e : hrService.getAllEmployees()) {
                empModel.addRow(new Object[]{
                    e.getEmployeeCode(), e.getFullName(), e.getHireDate(),
                    UIUtils.formatCurrency(e.getBaseSalary()), e.getStatus(), e.getEmployeeId()
                });
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void loadSchedule() {
        schedModel.setRowCount(0);
        String date = LocalDate.now().toString();
        try {
            java.util.Date d = (java.util.Date) spinDate.getValue();
            date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
        } catch (Exception ignored) {}
        try {
            for (ShiftAssignment sa : hrService.getSchedule(STORE_ID, date)) {
                schedModel.addRow(new Object[]{
                    sa.getEmployeeName(), sa.getShiftName(),
                    sa.getShiftStart(), sa.getShiftEnd(),
                    sa.getStatus(), sa.getShiftAssignmentId()
                });
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void loadAttendance() {
        attModel.setRowCount(0);
        try {
            for (AttendanceRecord a : hrService.getAttendance(STORE_ID, LocalDate.now().toString())) {
                attModel.addRow(new Object[]{
                    a.getEmployeeName(), a.getShiftName(),
                    a.getCheckInTime(), a.getCheckOutTime(), a.getWorkDate(),
                    a.getStatus(), a.getAttendanceId(), a.getShiftAssignmentId()
                });
            }
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void clockIn() {
        int row = tblSched.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn dòng lịch ca để chấm vào."); return; }
        String assignmentId = (String) schedModel.getValueAt(row, 5);
        try {
            hrService.clockIn(assignmentId);
            UIUtils.showSuccess(this, "Chấm vào thành công!");
            loadSchedule(); loadAttendance();
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void clockOut() {
        int row = tblAtt.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn dòng chấm công để chấm ra."); return; }
        String assignmentId = (String) attModel.getValueAt(row, 7);
        try {
            hrService.clockOut(assignmentId);
            UIUtils.showSuccess(this, "Chấm ra thành công!");
            loadAttendance();
        } catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    // ── Forms ──────────────────────────────────────────────────────
    private void openEmployeeForm(Employee existing) {
        JTextField txtCode   = new JTextField(existing != null ? existing.getEmployeeCode() : "");
        JTextField txtName   = new JTextField(existing != null ? existing.getFullName() : "");
        JTextField txtSalary = new JTextField(existing != null ? String.valueOf(existing.getBaseSalary()) : "");
        JComboBox<String> cboStatus = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE", "ON_LEAVE"});
        if (existing != null && existing.getStatus() != null) cboStatus.setSelectedItem(existing.getStatus());

        Object[] fields = {"Mã NV (*):", txtCode, "Họ tên (*):", txtName,
                           "Lương cơ bản:", txtSalary, "Trạng thái:", cboStatus};
        int res = JOptionPane.showConfirmDialog(this, fields,
                existing == null ? "Thêm nhân viên" : "Sửa nhân viên", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        Employee e = existing != null ? existing : new Employee();
        e.setEmployeeCode(txtCode.getText().trim());
        e.setFullName(txtName.getText().trim());
        e.setStatus((String) cboStatus.getSelectedItem());
        try {
            String salStr = txtSalary.getText().replace(",", "").trim();
            e.setBaseSalary(salStr.isEmpty() ? 0 : Double.parseDouble(salStr));
        } catch (NumberFormatException ignored) {}
        try { hrService.saveEmployee(e); loadEmployees(); UIUtils.showSuccess(this, "Lưu thành công!"); }
        catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }

    private void editSelectedEmployee() {
        int row = tblEmp.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Chọn nhân viên để sửa."); return; }
        Employee e = new Employee();
        e.setEmployeeId((String) empModel.getValueAt(row, 5));
        e.setEmployeeCode((String) empModel.getValueAt(row, 0));
        e.setFullName((String) empModel.getValueAt(row, 1));
        e.setStatus((String) empModel.getValueAt(row, 4));
        openEmployeeForm(e);
    }

    private void openAssignShiftDialog() {
        // Employees
        List<Employee> emps = new java.util.ArrayList<>();
        List<ShiftTemplate> shifts = new java.util.ArrayList<>();
        try { emps = hrService.getAllEmployees(); } catch (Exception ignored) {}
        try { shifts = hrService.getShiftTemplates(); } catch (Exception ignored) {}
        if (emps.isEmpty() || shifts.isEmpty()) {
            UIUtils.showError(this, "Chưa có nhân viên hoặc mẫu ca."); return;
        }

        Object[] empOpts = emps.stream().map(e -> e.getEmployeeCode() + " - " + e.getFullName()).toArray();
        Object[] shiftOpts = shifts.stream().map(s -> s.getName() + " (" + s.getStartTime() + "-" + s.getEndTime() + ")").toArray();

        JComboBox<Object> cboEmp   = new JComboBox<>(empOpts);
        JComboBox<Object> cboShift = new JComboBox<>(shiftOpts);
        JTextField txtDate = new JTextField(LocalDate.now().toString(), 12);

        Object[] fields = {"Nhân viên:", cboEmp, "Ca:", cboShift, "Ngày (yyyy-MM-dd):", txtDate};
        int res = JOptionPane.showConfirmDialog(this, fields, "Phân ca", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        ShiftAssignment sa = new ShiftAssignment();
        sa.setEmployeeId(emps.get(cboEmp.getSelectedIndex()).getEmployeeId());
        sa.setShiftTemplateId(shifts.get(cboShift.getSelectedIndex()).getShiftTemplateId());
        sa.setStoreId(STORE_ID);
        sa.setWorkDate(txtDate.getText().trim());
        try { hrService.assignShift(sa); loadSchedule(); UIUtils.showSuccess(this, "Phân ca thành công!"); }
        catch (Exception ex) { UIUtils.showError(this, ex.getMessage()); }
    }
}

