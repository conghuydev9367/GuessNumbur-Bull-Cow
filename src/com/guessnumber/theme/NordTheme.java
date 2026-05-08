package com.guessnumber.theme;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public final class NordTheme {

    public static final Color SNOW0  = new Color(0xEC, 0xEF, 0xF4);
    public static final Color SNOW1  = new Color(0xE5, 0xE9, 0xF0);
    public static final Color SNOW2  = new Color(0xD8, 0xDE, 0xE9);

    public static final Color DARK0  = new Color(0x2E, 0x34, 0x40);
    public static final Color DARK1  = new Color(0x3B, 0x42, 0x52);
    public static final Color DARK2  = new Color(0x43, 0x4C, 0x5E);
    public static final Color DARK3  = new Color(0x4C, 0x56, 0x6A);

    public static final Color FROST0 = new Color(0x8F, 0xBC, 0xBB);
    public static final Color FROST1 = new Color(0x88, 0xC0, 0xD0);
    public static final Color FROST2 = new Color(0x81, 0xA1, 0xC1);
    public static final Color FROST3 = new Color(0x5E, 0x81, 0xAC);

    public static final Color RED    = new Color(0xBF, 0x61, 0x6A);
    public static final Color ORANGE = new Color(0xD0, 0x87, 0x70);
    public static final Color YELLOW = new Color(0xEB, 0xCB, 0x8B);
    public static final Color GREEN  = new Color(0xA3, 0xBE, 0x8C);
    public static final Color PURPLE = new Color(0xB4, 0x8E, 0xAD);

    public static final Color BG      = new Color(0xF4, 0xF3, 0xEE);
    public static final Color BG_SEC  = SNOW1;
    public static final Color BORDER  = SNOW2;
    public static final Color FG      = new Color(0x2C, 0x2C, 0x2C);

    private NordTheme() {}

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        return l;
    }

    public static JLabel label(String text, Color fg) {
        JLabel l = new JLabel(text);
        l.setForeground(fg);
        return l;
    }

    public static JTextField textField(String text) {
        JTextField tf = new JTextField(text, 12);
        tf.setBackground(Color.WHITE);
        tf.setForeground(FG);
        tf.setCaretColor(FG);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return tf;
    }

    public static JTextArea textArea() {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setBackground(Color.WHITE);
        ta.setForeground(FG);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setSelectionColor(FROST1);
        ta.setSelectedTextColor(FG);
        ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return ta;
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JScrollPane titledScroll(JComponent view, String title) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER), title,
            TitledBorder.LEFT, TitledBorder.TOP,
            null, FROST3));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static void autoScroll(JTextArea ta) {
        ta.setCaretPosition(ta.getDocument().getLength());
    }
}
