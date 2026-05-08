package com.guessnumber.client.ui;

import com.guessnumber.client.GameClient;
import com.guessnumber.theme.NordTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class RoomPanel extends JPanel {

    private final GameClient client;
    private JPanel rootPanel;
    private JTextArea chatArea;
    private JTextField chatField, secretField, guessField;
    private JButton startBtn, setSecretBtn, guessBtn, sendChatBtn;
    private JLabel roomInfoLabel, turnLabel;
    
    // New UI elements
    private JPanel centerCards;
    private DefaultTableModel tableModel;
    private JLabel dashLabel, turnCountLabel;
    private String currentKeypadGuess = "";
    private int currentNumDigits = 4;
    private int guessCount = 0;

    public RoomPanel() {
        this(null);
    }

    public RoomPanel(GameClient client) {
        this.client = client;
        setLayout(new BorderLayout());
        buildUI();
        add(rootPanel, BorderLayout.CENTER);
    }

    private void buildUI() {
        rootPanel = new JPanel(new BorderLayout(5, 5));
        rootPanel.setBackground(NordTheme.BG);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // TOP PANEL
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(NordTheme.BG_SEC);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NordTheme.BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        roomInfoLabel = NordTheme.label("Phòng: ---");
        turnLabel = NordTheme.label("");
        turnLabel.setForeground(NordTheme.ORANGE);
        topPanel.add(roomInfoLabel, BorderLayout.WEST);
        topPanel.add(turnLabel, BorderLayout.EAST);
        rootPanel.add(topPanel, BorderLayout.NORTH);

        // CENTER CARDS (CHAT / GAME)
        centerCards = new JPanel(new CardLayout());
        
        // 1. Chat Card
        chatArea = NordTheme.textArea();
        centerCards.add(NordTheme.titledScroll(chatArea, "\uD83D\uDCAC Phòng Chat"), "CHAT");
        
        // 2. Game Card
        JPanel gamePanel = new JPanel(new BorderLayout(5, 5));
        gamePanel.setBackground(NordTheme.BG);
        
        // Game Top
        JPanel gameTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gameTop.setBackground(NordTheme.BG_SEC);
        turnCountLabel = NordTheme.label("Lần thứ: 0");
        turnCountLabel.setForeground(NordTheme.YELLOW);
        gameTop.add(turnCountLabel);
        JButton historyBtn = NordTheme.button("[ Lịch sử đoán ]", NordTheme.FROST3);
        historyBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) centerCards.getLayout();
            cl.show(centerCards, "HISTORY");
        });
        gameTop.add(historyBtn);
        gamePanel.add(gameTop, BorderLayout.NORTH);
        
        // Game Center (Dashes + Keypad)
        JPanel gameCenter = new JPanel(new BorderLayout());
        gameCenter.setBackground(NordTheme.BG);
        
        dashLabel = new JLabel("_ _ _ _", SwingConstants.CENTER);
        dashLabel.setFont(new Font("Monospaced", Font.BOLD, 36));
        dashLabel.setForeground(NordTheme.FG);
        dashLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        gameCenter.add(dashLabel, BorderLayout.NORTH);
        
        JPanel keypad = new JPanel(new GridLayout(4, 3, 10, 10));
        keypad.setBackground(NordTheme.BG);
        keypad.setBorder(BorderFactory.createEmptyBorder(0, 100, 20, 100)); // Padding to center
        String[] keys = {"1","2","3","4","5","6","7","8","9",".","0","<-"};
        for (String k : keys) {
            JButton btn = NordTheme.button(k, NordTheme.BG_SEC);
            btn.setForeground(NordTheme.FG);
            btn.setFont(new Font("Arial", Font.BOLD, 20));
            btn.addActionListener(e -> handleKeypad(k));
            keypad.add(btn);
        }
        gameCenter.add(keypad, BorderLayout.CENTER);
        gamePanel.add(gameCenter, BorderLayout.CENTER);
        
        // 3. History Card
        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBackground(NordTheme.BG);
        
        String[] columns = {"#", "Số đoán", "Bò", "Bê"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable historyTable = new JTable(tableModel);
        historyTable.setBackground(NordTheme.BG_SEC);
        historyTable.setForeground(NordTheme.FG);
        historyTable.getTableHeader().setBackground(NordTheme.FROST3);
        historyTable.getTableHeader().setForeground(Color.WHITE);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        
        JButton backBtn = NordTheme.button("Quay lại", NordTheme.FROST2);
        backBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) centerCards.getLayout();
            cl.show(centerCards, "GAME");
        });
        JPanel histBtm = new JPanel();
        histBtm.setBackground(NordTheme.BG);
        histBtm.add(backBtn);
        historyPanel.add(histBtm, BorderLayout.SOUTH);
        
        centerCards.add(historyPanel, "HISTORY");
        
        centerCards.add(gamePanel, "GAME");
        rootPanel.add(centerCards, BorderLayout.CENTER);

        // BOTTOM PANEL (Controls)
        JPanel btmAll = new JPanel(new BorderLayout(5, 5));
        btmAll.setBackground(NordTheme.BG);

        JPanel cw = new JPanel(new GridLayout(2, 1, 0, 4));
        cw.setBackground(NordTheme.BG);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row1.setBackground(NordTheme.BG_SEC);
        row1.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NordTheme.BORDER),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        startBtn = NordTheme.button("\u25B6 Bắt đầu", NordTheme.GREEN);
        startBtn.addActionListener(e -> client.startGame());
        row1.add(startBtn);
        row1.add(NordTheme.label(" Số bí mật:"));
        secretField = NordTheme.textField("");
        secretField.setPreferredSize(new Dimension(120, 28));
        row1.add(secretField);
        setSecretBtn = NordTheme.button("\u2705 Đặt", NordTheme.FROST3);
        setSecretBtn.setEnabled(false);
        setSecretBtn.addActionListener(e -> {
            String s = secretField.getText().trim();
            if (!s.isEmpty()) client.setSecret(s);
        });
        row1.add(setSecretBtn);
        cw.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row2.setBackground(NordTheme.BG_SEC);
        row2.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NordTheme.BORDER),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        row2.add(NordTheme.label("Đoán số:"));
        guessField = NordTheme.textField("");
        guessField.setPreferredSize(new Dimension(150, 28));
        guessField.addActionListener(e -> submitGuess());
        row2.add(guessField);
        guessBtn = NordTheme.button("\uD83C\uDFAF Đoán", NordTheme.RED);
        guessBtn.setEnabled(false);
        guessBtn.addActionListener(e -> submitGuess());
        row2.add(guessBtn);
        cw.add(row2);
        btmAll.add(cw, BorderLayout.NORTH);

        JPanel chatRow = new JPanel(new BorderLayout(5, 0));
        chatRow.setBackground(NordTheme.BG);
        chatField = NordTheme.textField("");
        chatField.addActionListener(e -> sendRoomChat());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnRow.setBackground(NordTheme.BG);
        sendChatBtn = NordTheme.button("Gửi", NordTheme.FROST3);
        sendChatBtn.addActionListener(e -> sendRoomChat());
        JButton leaveBtn = NordTheme.button("\uD83D\uDEAA Rời", NordTheme.RED);
        leaveBtn.addActionListener(e -> client.leaveRoom());
        btnRow.add(sendChatBtn);
        btnRow.add(leaveBtn);
        chatRow.add(chatField, BorderLayout.CENTER);
        chatRow.add(btnRow, BorderLayout.EAST);
        btmAll.add(chatRow, BorderLayout.SOUTH);
        rootPanel.add(btmAll, BorderLayout.SOUTH);
    }
    
    private void handleKeypad(String k) {
        if (k.equals("<-")) {
            if (currentKeypadGuess.length() > 0) {
                currentKeypadGuess = currentKeypadGuess.substring(0, currentKeypadGuess.length() - 1);
            }
        } else if (!k.equals(".")) {
            if (currentKeypadGuess.length() < currentNumDigits) {
                currentKeypadGuess += k;
            }
        }
        updateDashDisplay();
        guessField.setText(currentKeypadGuess);
    }
    
    private void updateDashDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentNumDigits; i++) {
            if (i < currentKeypadGuess.length()) sb.append(currentKeypadGuess.charAt(i)).append(" ");
            else sb.append("_ ");
        }
        dashLabel.setText(sb.toString().trim());
    }

    public void showGameLayout(int numDigits) {
        this.currentNumDigits = numDigits;
        this.currentKeypadGuess = "";
        this.guessCount = 0;
        tableModel.setRowCount(0);
        turnCountLabel.setText("Lần thứ: 0");
        updateDashDisplay();
        CardLayout cl = (CardLayout) centerCards.getLayout();
        cl.show(centerCards, "GAME");
    }
    
    public void resetToChatLayout() {
        CardLayout cl = (CardLayout) centerCards.getLayout();
        cl.show(centerCards, "CHAT");
    }
    
    public void addHistory(String guess, String bo, String be) {
        guessCount++;
        turnCountLabel.setText("Lần thứ: " + guessCount);
        tableModel.addRow(new Object[]{guessCount, guess, bo, be});
    }

    private void submitGuess() {
        String g = guessField.getText().trim();
        if (!g.isEmpty() && client.isMyTurn()) {
            client.guess(g);
            currentKeypadGuess = "";
            updateDashDisplay();
            guessField.setText("");
        }
    }

    private void sendRoomChat() {
        String msg = chatField.getText().trim();
        if (!msg.isEmpty()) {
            client.roomChat(msg);
            chatField.setText("");
        }
    }

    public void setRoomInfo(String text) { roomInfoLabel.setText(text); }
    public void setTurnText(String text) { turnLabel.setText(text); }
    public void clearChat() { chatArea.setText(""); }
    public void appendChat(String text) { chatArea.append(text); NordTheme.autoScroll(chatArea); }
    public void setStartEnabled(boolean b) { startBtn.setEnabled(b); }
    public void setSecretEnabled(boolean b) { setSecretBtn.setEnabled(b); }
    public void setSecretEditable(boolean b) { secretField.setEditable(b); }
    public void setGuessEnabled(boolean b) { guessBtn.setEnabled(b); }
    public void setGuessEditable(boolean b) { guessField.setEditable(b); }
    public void focusGuess() { guessField.requestFocus(); }
    public void setChatEnabled(boolean b) { chatField.setEditable(b); sendChatBtn.setEnabled(b); }
}
