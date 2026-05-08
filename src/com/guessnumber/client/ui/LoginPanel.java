package com.guessnumber.client.ui;

import com.guessnumber.client.GameClient;
import com.guessnumber.theme.NordTheme;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    private final GameClient client;
    private JPanel rootPanel;
    private JTextField nickField;
    private JTextField serverIpField;

    public LoginPanel() {
        this(null);
    }

    public LoginPanel(GameClient client) {
        this.client = client;
        setLayout(new BorderLayout());
        buildUI();
        add(rootPanel, BorderLayout.CENTER);
    }

    private void buildUI() {
        rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(NordTheme.BG);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLbl = new JLabel("\uD83C\uDFAE GAME ĐOÁN SỐ", SwingConstants.CENTER);
        titleLbl.setForeground(NordTheme.FROST3);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        rootPanel.add(titleLbl, g);

        JLabel subLbl = new JLabel("Multiplayer LAN", SwingConstants.CENTER);
        subLbl.setForeground(NordTheme.DARK3);
        g.gridy = 1;
        rootPanel.add(subLbl, g);

        g.gridwidth = 1; g.gridy = 2; g.gridx = 0;
        rootPanel.add(NordTheme.label("Nickname:"), g);
        g.gridx = 1;
        nickField = NordTheme.textField("Player");
        rootPanel.add(nickField, g);

        g.gridy = 3; g.gridx = 0;
        rootPanel.add(NordTheme.label("Server IP:"), g);
        g.gridx = 1;
        serverIpField = NordTheme.textField("auto");
        rootPanel.add(serverIpField, g);

        g.gridy = 4; g.gridx = 0; g.gridwidth = 2;
        JButton connectBtn = NordTheme.button("\uD83D\uDD17 Kết nối", NordTheme.FROST3);
        connectBtn.addActionListener(e -> client.connectToServer());
        rootPanel.add(connectBtn, g);

        g.gridy = 5;
        JButton discoverBtn = NordTheme.button("\uD83D\uDCE1 Tìm Server (LAN)", NordTheme.PURPLE);
        discoverBtn.addActionListener(e -> client.discoverServer());
        rootPanel.add(discoverBtn, g);
    }

    public String getNickname() { return nickField.getText().trim(); }
    public String getServerIp() { return serverIpField.getText().trim(); }
    public void setServerIp(String ip) { serverIpField.setText(ip); }
}
