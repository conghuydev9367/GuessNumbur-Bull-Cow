package com.guessnumber.client.ui;

import com.guessnumber.client.GameClient;
import com.guessnumber.theme.NordTheme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class LobbyPanel extends JPanel {

    private final GameClient client;
    private JPanel rootPanel;
    private JTextPane chatArea;
    private JTextField chatField;
    private JTextField roomCodeField;
    private JSpinner digitSpinner;
    private JProgressBar progressBar1;

    public LobbyPanel() {
        this(null);
    }

    public LobbyPanel(GameClient client) {
        this.client = client;
        setLayout(new BorderLayout());
        buildUI();
        add(rootPanel, BorderLayout.CENTER);
    }

    private void buildUI() {
        rootPanel = new JPanel(new BorderLayout(5, 5));
        rootPanel.setBackground(NordTheme.BG);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topPanel.setBackground(NordTheme.BG_SEC);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NordTheme.BORDER),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        topPanel.add(NordTheme.label("Số chữ số:"));
        digitSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 10, 1));
        digitSpinner.setPreferredSize(new Dimension(50, 28));
        topPanel.add(digitSpinner);

        topPanel.add(NordTheme.label("Thời gian (s):"));
        JSpinner timeSpinner = new JSpinner(new SpinnerNumberModel(20, 10, 40, 1));
        timeSpinner.setPreferredSize(new Dimension(50, 28));
        topPanel.add(timeSpinner);

        JButton createBtn = NordTheme.button("\uD83C\uDFE0 Tạo Phòng", NordTheme.GREEN);
        createBtn.addActionListener(e -> {
            int d = (Integer) digitSpinner.getValue();
            int t = (Integer) timeSpinner.getValue();
            client.createRoom(d, t);
        });
        topPanel.add(createBtn);

        topPanel.add(NordTheme.label("  Mã phòng:"));
        roomCodeField = NordTheme.textField("");
        roomCodeField.setPreferredSize(new Dimension(80, 28));
        topPanel.add(roomCodeField);

        JButton joinBtn = NordTheme.button("\uD83D\uDEAA Vào Phòng", NordTheme.ORANGE);
        joinBtn.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (!code.isEmpty()) client.joinRoom(code);
        });
        topPanel.add(joinBtn);
        rootPanel.add(topPanel, BorderLayout.NORTH);

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(Color.WHITE);
        chatArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rootPanel.add(NordTheme.titledScroll(chatArea, "\uD83D\uDCAC Lobby Chat"), BorderLayout.CENTER);

        JPanel btm = new JPanel(new BorderLayout(5, 0));
        btm.setBackground(NordTheme.BG);
        chatField = NordTheme.textField("");
        chatField.addActionListener(e -> sendChat());
        JButton sendBtn = NordTheme.button("Gửi", NordTheme.FROST3);
        sendBtn.addActionListener(e -> sendChat());
        btm.add(chatField, BorderLayout.CENTER);
        btm.add(sendBtn, BorderLayout.EAST);
        rootPanel.add(btm, BorderLayout.SOUTH);
    }

    private void sendChat() {
        String msg = chatField.getText().trim();
        if (!msg.isEmpty()) {
            client.sendChat(msg);
            chatField.setText("");
        }
    }

    public void appendText(String text, Color color) {
        StyledDocument doc = chatArea.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (BadLocationException ignored) {}
        chatArea.setCaretPosition(doc.getLength());
    }

    public void appendJoinButton(String roomCode) {
        JButton joinBtn = new JButton("\u25B6 Join " + roomCode);
        joinBtn.setBackground(NordTheme.FROST3);
        joinBtn.setForeground(Color.WHITE);
        joinBtn.setFocusPainted(false);
        joinBtn.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        joinBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        joinBtn.addActionListener(e -> {
            if (client.getCurrentRoom() == null) {
                client.joinRoom(roomCode);
            } else {
                JOptionPane.showMessageDialog(client,
                    "Bạn đang ở trong phòng rồi!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });
        chatArea.setCaretPosition(chatArea.getStyledDocument().getLength());
        chatArea.insertComponent(joinBtn);
    }
}
