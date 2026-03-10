package org.example.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Shared UI constants — Vật Liệu Xây Dựng (Construction Materials) theme.
 * Palette: Brick-orange header, Concrete-grey bg, Steel-blue accent.
 */
public final class UIUtils {

    // ── Color Palette — Construction / Industrial ──────────────────
    /** Brick red-orange — primary brand color */
    public static final Color COLOR_PRIMARY    = new Color(180, 60, 20);
    /** Steel blue — secondary / action */
    public static final Color COLOR_SECONDARY  = new Color(30, 90, 150);
    /** Red — danger/delete */
    public static final Color COLOR_DANGER     = new Color(200, 30, 30);
    /** Olive green — success/confirm */
    public static final Color COLOR_SUCCESS    = new Color(70, 130, 60);
    /** Amber — warning / low stock */
    public static final Color COLOR_WARNING    = new Color(210, 130, 0);
    /** Concrete light grey — main background */
    public static final Color COLOR_BG         = new Color(240, 238, 234);
    /** Alternating table row — very light sand */
    public static final Color COLOR_TABLE_EVEN = new Color(255, 248, 235);
    /** Low stock row highlight — pale red */
    public static final Color COLOR_LOW_STOCK  = new Color(255, 215, 200);
    /** Sidebar background — dark charcoal */
    public static final Color COLOR_SIDEBAR    = new Color(38, 35, 30);
    /** Sidebar button hover */
    public static final Color COLOR_SIDEBAR_HOVER = new Color(180, 60, 20);
    /** Top header bar */
    public static final Color COLOR_TOPBAR     = new Color(55, 48, 40);
    /** Card / panel background */
    public static final Color COLOR_CARD       = new Color(255, 253, 248);
    /** Border color */
    public static final Color COLOR_BORDER     = new Color(200, 185, 165);
    /** Muted text */
    public static final Color COLOR_TEXT_MUTED = new Color(120, 110, 95);

    // ── Fonts ──────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_MONO    = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FONT_LARGE   = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);

    private UIUtils() {}

    /** Styled primary button (brick-orange). */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    /** Styled danger (red) button. */
    public static JButton dangerButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(COLOR_DANGER);
        return btn;
    }

    /** Styled success (green) button. */
    public static JButton successButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(COLOR_SUCCESS);
        return btn;
    }

    /** Styled secondary (steel-blue) button. */
    public static JButton secondaryButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(COLOR_SECONDARY);
        return btn;
    }

    /** Warning (amber) button. */
    public static JButton warningButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(COLOR_WARNING);
        return btn;
    }

    /** Section header label. */
    public static JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(COLOR_PRIMARY);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        return lbl;
    }

    /** Build a titled card panel (white card with colored left border). */
    public static JPanel cardPanel(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, COLOR_PRIMARY),
            BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
            )
        ));
        if (title != null && !title.isEmpty()) {
            JLabel lbl = new JLabel(title);
            lbl.setFont(FONT_BOLD);
            lbl.setForeground(COLOR_PRIMARY);
            card.add(lbl, BorderLayout.NORTH);
        }
        return card;
    }

    /** Formatted currency: 15,000 ₫ */
    public static String formatCurrency(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    /** Show error dialog. */
    public static void showError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Show success dialog. */
    public static void showSuccess(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Style a JTable for the construction theme. */
    public static void styleTable(JTable table) {
        table.setFont(FONT_LABEL);
        table.setRowHeight(28);
        table.setGridColor(COLOR_BORDER);
        table.setBackground(COLOR_CARD);
        table.setSelectionBackground(new Color(180, 60, 20, 60));
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(COLOR_CARD);
        table.getTableHeader().setForeground(Color.BLACK);
        table.getTableHeader().setReorderingAllowed(false);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
    }

    /** Style a text field for construction theme. */
    public static void styleField(JTextField tf) {
        tf.setFont(FONT_LABEL);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)
        ));
        tf.setBackground(Color.WHITE);
    }

    /**
     * Apply zebra-stripe (alternating row color) renderer to a table.
     * Also applies construction theme header styling.
     */
    public static void applyZebraRenderer(JTable table) {
        styleTable(table);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setFont(FONT_LABEL);
                setForeground(Color.BLACK);
                if (sel) {
                    setBackground(new Color(180, 60, 20, 60));
                    setForeground(Color.BLACK);
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : COLOR_TABLE_EVEN);
                }
                setBorder(new EmptyBorder(2, 6, 2, 6));
                return this;
            }
        });
        // Integer renderer — right-aligned
        table.setDefaultRenderer(Integer.class, new javax.swing.table.DefaultTableCellRenderer() {
            { setHorizontalAlignment(javax.swing.SwingConstants.RIGHT); }
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setFont(FONT_LABEL);
                setForeground(Color.BLACK);
                if (sel) setBackground(new Color(180, 60, 20, 60));
                else      setBackground(row % 2 == 0 ? Color.WHITE : COLOR_TABLE_EVEN);
                return this;
            }
        });
    }

    /**
     * Create a titled panel with a brick-orange top border strip.
     * Compatible with any LayoutManager.
     */
    public static JPanel titledPanel(String title, java.awt.LayoutManager layout) {
        JPanel outer = new JPanel(new BorderLayout(0, 4));
        outer.setBackground(COLOR_CARD);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 0, 0, COLOR_PRIMARY),
            BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
            )
        ));
        if (title != null && !title.isEmpty()) {
            JLabel hdr = new JLabel(title);
            hdr.setFont(FONT_BOLD);
            hdr.setForeground(COLOR_PRIMARY);
            hdr.setBorder(new EmptyBorder(0, 0, 6, 0));
            outer.add(hdr, BorderLayout.NORTH);
        }
        JPanel inner = new JPanel(layout);
        inner.setBackground(COLOR_CARD);
        outer.add(inner, BorderLayout.CENTER);
        // Expose inner panel methods via delegation pattern is complex;
        // instead return outer and callers add to it directly.
        outer.setLayout(layout);  // override so callers can add directly
        outer.setBackground(COLOR_CARD);
        return outer;
    }

    /** Show a yes/no confirmation dialog. Returns true if user chose YES. */
    public static boolean confirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(
            parent, message, "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
