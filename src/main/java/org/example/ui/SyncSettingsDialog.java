package org.example.ui;

import org.example.service.SyncService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Dialog cho phép cấu hình kết nối sync Desktop ↔ Cloud.
 *
 * ┌──────────────────────────────────────────────┐
 * │  ☁  CÀI ĐẶT ĐỒNG BỘ DỮ LIỆU               │
 * ├──────────────────────────────────────────────┤
 * │  Server URL:   [https://xxx.railway.app    ] │
 * │  Mã cửa hàng:  [STORE-001                 ] │
 * │  API Key:      [xxxxxxxx                   ] │
 * │                                              │
 * │  [Đăng ký mới]  [Kiểm tra kết nối]          │
 * ├──────────────────────────────────────────────┤
 * │  Trạng thái:    ● SYNCED ✓                   │
 * │  Lần sync cuối: 10/03/2026 21:30:00          │
 * │  Chờ đồng bộ:   5 bản ghi                    │
 * ├──────────────────────────────────────────────┤
 * │  Auto-sync:     [✓] mỗi [60] giây            │
 * │                                              │
 * │  [Sync ngay]  [Lưu]  [Đóng]                  │
 * └──────────────────────────────────────────────┘
 */
public class SyncSettingsDialog extends JDialog {

    private final SyncService syncService = SyncService.getInstance();

    private final JTextField txtServerUrl = new JTextField(30);
    private final JTextField txtStoreId   = new JTextField(20);
    private final JTextField txtApiKey    = new JTextField(30);
    private final JCheckBox  chkAutoSync  = new JCheckBox("Tự động đồng bộ");
    private final JSpinner   spnInterval  = new JSpinner(new SpinnerNumberModel(60, 10, 3600, 10));

    private final JLabel lblStatus      = new JLabel("—");
    private final JLabel lblLastSync    = new JLabel("—");
    private final JLabel lblPending     = new JLabel("—");
    private final JLabel lblTestResult  = new JLabel(" ");

    public SyncSettingsDialog(Window owner) {
        super(owner, "☁  Cài Đặt Đồng Bộ Dữ Liệu", ModalityType.APPLICATION_MODAL);
        setSize(520, 540);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
        loadCurrentConfig();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setBackground(new Color(245, 240, 230));

        // ── Connection Panel ──
        JPanel connPanel = new JPanel(new GridBagLayout());
        connPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 160, 130)),
            " Kết Nối Server ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13), new Color(60, 50, 40)));
        connPanel.setBackground(new Color(250, 245, 238));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx=0; gbc.gridy=0;
        connPanel.add(new JLabel("Server URL:"), gbc);
        gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1;
        txtServerUrl.setToolTipText("VD: https://your-app.railway.app");
        connPanel.add(txtServerUrl, gbc);

        gbc.gridx=0; gbc.gridy=1; gbc.fill=GridBagConstraints.NONE; gbc.weightx=0;
        connPanel.add(new JLabel("Mã cửa hàng:"), gbc);
        gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1;
        connPanel.add(txtStoreId, gbc);

        gbc.gridx=0; gbc.gridy=2; gbc.fill=GridBagConstraints.NONE; gbc.weightx=0;
        connPanel.add(new JLabel("API Key:"), gbc);
        gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1;
        connPanel.add(txtApiKey, gbc);

        // Buttons row
        JPanel btnRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow1.setOpaque(false);

        JButton btnRegister = new JButton("📝 Đăng ký mới");
        btnRegister.setToolTipText("Đăng ký cửa hàng với server để lấy API Key");
        btnRegister.addActionListener(e -> doRegister());
        btnRow1.add(btnRegister);

        JButton btnTest = new JButton("🔗 Kiểm tra kết nối");
        btnTest.addActionListener(e -> doTestConnection());
        btnRow1.add(btnTest);

        lblTestResult.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        btnRow1.add(lblTestResult);

        gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2;
        connPanel.add(btnRow1, gbc);

        main.add(connPanel, BorderLayout.NORTH);

        // ── Status Panel ──
        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 160, 130)),
            " Trạng Thái Đồng Bộ ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13), new Color(60, 50, 40)));
        statusPanel.setBackground(new Color(250, 245, 238));

        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(4, 8, 4, 8);
        g2.anchor = GridBagConstraints.WEST;

        g2.gridx=0; g2.gridy=0;
        statusPanel.add(new JLabel("Trạng thái:"), g2);
        g2.gridx=1;
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusPanel.add(lblStatus, g2);

        g2.gridx=0; g2.gridy=1;
        statusPanel.add(new JLabel("Lần sync cuối:"), g2);
        g2.gridx=1;
        statusPanel.add(lblLastSync, g2);

        g2.gridx=0; g2.gridy=2;
        statusPanel.add(new JLabel("Chờ đồng bộ:"), g2);
        g2.gridx=1;
        statusPanel.add(lblPending, g2);

        // Auto-sync config
        g2.gridx=0; g2.gridy=3; g2.gridwidth=2;
        JPanel autoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        autoPanel.setOpaque(false);
        chkAutoSync.setOpaque(false);
        chkAutoSync.setSelected(true);
        autoPanel.add(chkAutoSync);
        autoPanel.add(new JLabel("mỗi"));
        spnInterval.setPreferredSize(new Dimension(70, 26));
        autoPanel.add(spnInterval);
        autoPanel.add(new JLabel("giây"));
        statusPanel.add(autoPanel, g2);

        main.add(statusPanel, BorderLayout.CENTER);

        // ── Bottom Buttons ──
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnSyncNow = new JButton("🔄 Sync ngay");
        btnSyncNow.setBackground(new Color(46, 125, 50));
        btnSyncNow.setForeground(Color.WHITE);
        btnSyncNow.setFocusPainted(false);
        btnSyncNow.addActionListener(e -> doSyncNow());
        btnPanel.add(btnSyncNow);

        JButton btnSave = new JButton("💾 Lưu cài đặt");
        btnSave.setBackground(new Color(30, 100, 180));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> doSave());
        btnPanel.add(btnSave);

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dispose());
        btnPanel.add(btnClose);

        main.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(main);
    }

    private void loadCurrentConfig() {
        if (syncService.getServerUrl() != null) txtServerUrl.setText(syncService.getServerUrl());
        if (syncService.getStoreId() != null)   txtStoreId.setText(syncService.getStoreId());
        if (syncService.getApiKey() != null)     txtApiKey.setText(syncService.getApiKey());
        updateStatusLabels();
    }

    private void updateStatusLabels() {
        SyncService.SyncState state = syncService.getState();
        switch (state) {
            case SYNCED  -> { lblStatus.setText("● ĐỒNG BỘ ✓"); lblStatus.setForeground(new Color(46,125,50)); }
            case SYNCING -> { lblStatus.setText("● ĐANG ĐỒNG BỘ..."); lblStatus.setForeground(new Color(255,152,0)); }
            case OFFLINE -> { lblStatus.setText("● OFFLINE"); lblStatus.setForeground(new Color(158,158,158)); }
            case ERROR   -> { lblStatus.setText("● LỖI: " + syncService.getLastError()); lblStatus.setForeground(Color.RED); }
            default      -> { lblStatus.setText("● CHƯA CẤU HÌNH"); lblStatus.setForeground(Color.GRAY); }
        }

        Instant last = syncService.getLastSyncTime();
        if (last != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
            lblLastSync.setText(fmt.format(last));
        } else {
            lblLastSync.setText("Chưa đồng bộ");
        }

        lblPending.setText(syncService.getPendingCount() + " bản ghi");
    }

    private void doRegister() {
        String url = txtServerUrl.getText().trim();
        String storeId = txtStoreId.getText().trim();

        if (url.isEmpty() || storeId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập Server URL và Mã cửa hàng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Remove trailing slash
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        lblTestResult.setText("Đang đăng ký...");

        String finalUrl = url;
        new SwingWorker<Map<String, String>, Void>() {
            @Override protected Map<String, String> doInBackground() throws Exception {
                return syncService.registerWithServer(finalUrl, storeId, storeId);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Map<String, String> result = get();
                    txtApiKey.setText(result.get("apiKey"));
                    lblTestResult.setText("✅ Đăng ký thành công!");
                    lblTestResult.setForeground(new Color(46, 125, 50));
                    JOptionPane.showMessageDialog(SyncSettingsDialog.this,
                        "Đăng ký thành công!\nAPI Key: " + result.get("apiKey"),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    lblTestResult.setText("❌ Lỗi: " + e.getCause().getMessage());
                    lblTestResult.setForeground(Color.RED);
                }
            }
        }.execute();
    }

    private void doTestConnection() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        lblTestResult.setText("Đang kiểm tra...");

        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() throws Exception {
                // Temporarily set URL for testing
                String url = txtServerUrl.getText().trim();
                if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
                syncService.configure(url, txtStoreId.getText().trim(), txtApiKey.getText().trim());
                return syncService.checkServerStatus();
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Map<String, Object> status = get();
                    lblTestResult.setText("✅ Kết nối OK — Server: " + status.get("status"));
                    lblTestResult.setForeground(new Color(46, 125, 50));
                } catch (Exception e) {
                    lblTestResult.setText("❌ Không kết nối được: " + e.getCause().getMessage());
                    lblTestResult.setForeground(Color.RED);
                }
            }
        }.execute();
    }

    private void doSyncNow() {
        if (!syncService.isConfigured()) {
            JOptionPane.showMessageDialog(this, "Chưa cấu hình sync!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        lblStatus.setText("● ĐANG ĐỒNG BỘ...");
        lblStatus.setForeground(new Color(255, 152, 0));

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                syncService.syncAll();
                return null;
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                updateStatusLabels();
                if (syncService.getState() == SyncService.SyncState.SYNCED) {
                    JOptionPane.showMessageDialog(SyncSettingsDialog.this,
                        "Đồng bộ thành công!", "Sync", JOptionPane.INFORMATION_MESSAGE);
                } else if (syncService.getState() == SyncService.SyncState.ERROR) {
                    JOptionPane.showMessageDialog(SyncSettingsDialog.this,
                        "Lỗi đồng bộ: " + syncService.getLastError(),
                        "Sync Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doSave() {
        String url = txtServerUrl.getText().trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        syncService.configure(url, txtStoreId.getText().trim(), txtApiKey.getText().trim());

        if (chkAutoSync.isSelected()) {
            int interval = (int) spnInterval.getValue();
            syncService.startAutoSync(interval);
        } else {
            syncService.stopAutoSync();
        }

        JOptionPane.showMessageDialog(this, "Đã lưu cài đặt đồng bộ.", "Lưu", JOptionPane.INFORMATION_MESSAGE);
        updateStatusLabels();
    }
}

