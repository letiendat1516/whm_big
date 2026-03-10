package org.example.ui.pos;

import org.example.model.Customer;
import org.example.service.CRMService;
import org.example.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Quick customer registration dialog from POS screen. */
public class CustomerQuickDialog extends JDialog {

    private final CRMService crmService = new CRMService();
    private Customer result = null;

    private final JTextField txtName  = new JTextField(20);
    private final JTextField txtPhone = new JTextField(15);
    private final JTextField txtEmail = new JTextField(20);

    public CustomerQuickDialog(JFrame parent, Customer prefill) {
        super(parent, "Đăng ký khách hàng mới", true);
        setSize(400, 260);
        setLocationRelativeTo(parent);
        buildUI();
        if (prefill != null && prefill.getPhoneNum() != null) txtPhone.setText(prefill.getPhoneNum());
    }

    private void buildUI() {
        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; main.add(new JLabel("Họ tên (*)"), g);
        g.gridx=1; main.add(txtName, g);
        g.gridx=0; g.gridy=1; main.add(new JLabel("Điện thoại (*)"), g);
        g.gridx=1; main.add(txtPhone, g);
        g.gridx=0; g.gridy=2; main.add(new JLabel("Email"), g);
        g.gridx=1; main.add(txtEmail, g);

        JButton btnSave  = UIUtils.successButton("Lưu");
        JButton btnClose = UIUtils.dangerButton("Hủy");
        JPanel bot = new JPanel();
        bot.add(btnSave); bot.add(btnClose);
        g.gridx=0; g.gridy=3; g.gridwidth=2; main.add(bot, g);

        btnSave.addActionListener(e -> save());
        btnClose.addActionListener(e -> dispose());
        setContentPane(main);
    }

    private void save() {
        Customer c = new Customer();
        c.setFullName(txtName.getText().trim());
        c.setPhoneNum(txtPhone.getText().trim());
        c.setEmail(txtEmail.getText().trim());
        try {
            crmService.saveCustomer(c);
            result = crmService.lookupByPhone(c.getPhoneNum());
            UIUtils.showSuccess(this, "Đăng ký thành công!");
            dispose();
        } catch (Exception ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    public Customer getResult() { return result; }
}

