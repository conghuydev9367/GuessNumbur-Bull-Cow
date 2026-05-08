package com.guessnumber.client;

import com.guessnumber.client.ui.LobbyPanel;
import com.guessnumber.client.ui.LoginPanel;
import com.guessnumber.client.ui.RoomPanel;
import com.guessnumber.common.Message;
import com.guessnumber.theme.NordTheme;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;

public class GameClient extends JFrame {

    private static InetAddress host;
    private static final int TCP_PORT = 9876;
    private static final int UDP_PORT = 9877;
    private static final String DISCOVER_REQ = "GAME_DISCOVER";

    private String nickname;
    private String currentRoom;
    private boolean isOwner = false;
    private int numDigits = 4;
    private boolean myTurn = false;
    private javax.swing.Timer turnTimer;
    private int timeLeft;
    private String lastGuess = "";

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private LobbyPanel lobbyPanel;
    private RoomPanel roomPanel;

    private Socket link;
    private PrintWriter output;
    private Scanner input;
    private volatile boolean connected = false;

    public GameClient() {
        setupGUI();
    }

    private void setupGUI() {
        setTitle("\uD83C\uDFAE Game \u0110o\u00e1n S\u1ed1");
        setSize(700, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(NordTheme.BG);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(NordTheme.BG);

        loginPanel = new LoginPanel(this);
        lobbyPanel = new LobbyPanel(this);
        roomPanel = new RoomPanel(this);

        mainPanel.add(loginPanel, "login");
        mainPanel.add(lobbyPanel, "lobby");
        mainPanel.add(roomPanel, "room");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    private void stopLocalTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
            turnTimer = null;
        }
    }

    public void send(Message msg) {
        if (output != null && msg != null) {
            output.println(msg.serialize());
        }
    }

    public void sendChat(String content) {
        send(Message.of("CHAT", content));
    }

    public void createRoom(int digits, int turnTime) {
        send(Message.of("CREATE_ROOM", String.valueOf(digits), String.valueOf(turnTime)));
    }

    public void joinRoom(String roomCode) {
        send(Message.of("JOIN_ROOM", roomCode));
    }

    public void leaveRoom() {
        send(Message.of("LEAVE_ROOM", ""));
    }

    public void roomChat(String content) {
        send(Message.of("ROOM_CHAT", content));
    }

    public void startGame() {
        send(Message.of("START_GAME", ""));
    }

    public void setSecret(String secret) {
        send(Message.of("SET_SECRET", secret));
    }

    public void guess(String value) {
        lastGuess = value;
        send(Message.of("GUESS", value));
    }

    public void discoverServer() {
        new Thread(() -> {
            try {
                DatagramSocket ds = new DatagramSocket();
                ds.setBroadcast(true);
                ds.setSoTimeout(3000);
                byte[] data = DISCOVER_REQ.getBytes();
                DatagramPacket pkt = new DatagramPacket(data, data.length,
                        InetAddress.getByName("255.255.255.255"), UDP_PORT);
                ds.send(pkt);
                byte[] buf = new byte[256];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);
                ds.receive(resp);
                String raw = new String(resp.getData(), 0, resp.getLength());
                Message msg = Message.parse(raw);
                if ("GAME_SERVER".equals(msg.command()) && msg.fieldCount() >= 2) {
                    String ip = msg.field(1);
                    SwingUtilities.invokeLater(() -> {
                        loginPanel.setServerIp(ip);
                        JOptionPane.showMessageDialog(this,
                                "Tìm thấy server: " + ip, "\uD83D\uDCE1 Phát hiện",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                }
                ds.close();
            } catch (SocketTimeoutException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Không tìm thấy server trên mạng LAN!", "Lỗi",
                        JOptionPane.WARNING_MESSAGE));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Lỗi: " + e.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    public void connectToServer() {
        String nick = loginPanel.getNickname();
        if (nick.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hãy nhập nickname!", "Lỗi",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String ip = loginPanel.getServerIp();
        if (ip.equals("auto") || ip.isEmpty()) {
            discoverServer();
            return;
        }

        new Thread(() -> {
            try {
                host = InetAddress.getByName(ip);
                link = new Socket(host, TCP_PORT);
                input = new Scanner(link.getInputStream());
                output = new PrintWriter(link.getOutputStream(), true);
                connected = true;
                nickname = nick;
                send(Message.of("NICK", nick));
                new Thread(this::listenServer, "Listener").start();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Không thể kết nối: " + e.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void listenServer() {
        try {
            while (connected && input.hasNextLine()) {
                String line = input.nextLine();
                handleServerMessage(line);
            }
        } catch (Exception e) {
            if (connected) {
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Mất kết nối với server!");
                    cardLayout.show(mainPanel, "login");
                });
            }
        }
    }

    private void handleServerMessage(String line) {
        Message msg = Message.parse(line);
        String cmd = msg.command();

        SwingUtilities.invokeLater(() -> {
            switch (cmd) {
                case "WELCOME":
                    break;
                case "NICK_OK":
                    cardLayout.show(mainPanel, "lobby");
                    setTitle("\uD83C\uDFAE Game \u0110o\u00e1n S\u1ed1 - " + nickname);
                    break;
                case "CHAT_MSG":
                    if (msg.fieldCount() >= 2) {
                        lobbyPanel.appendText(msg.field(0) + ": " + msg.field(1) + "\n", NordTheme.DARK0);
                    }
                    break;
                case "ROOM_ANNOUNCE":
                    if (msg.fieldCount() >= 3) {
                        String roomCode = msg.field(0);
                        String owner = msg.field(1);
                        String digits = msg.field(2);
                        String time = msg.fieldCount() >= 4 ? msg.field(3) : "20";
                        lobbyPanel.appendText("\uD83C\uDFAE " + owner +
                                " đã tạo phòng [" + roomCode + "] - " + digits +
                                " chữ số (" + time + "s/lượt)  ", NordTheme.FROST3);
                        lobbyPanel.appendJoinButton(roomCode);
                        lobbyPanel.appendText("\n", NordTheme.DARK0);
                    }
                    break;
                case "ROOM_CREATED":
                    currentRoom = msg.field(0);
                    isOwner = true;
                    roomPanel.setRoomInfo("Phòng: " + currentRoom + " (Chủ phòng)");
                    roomPanel.clearChat();
                    roomPanel.appendChat("\uD83C\uDFE0 Bạn đã tạo phòng " + currentRoom + "\n");
                    roomPanel.setStartEnabled(true);
                    roomPanel.setSecretEnabled(false);
                    roomPanel.setGuessEnabled(false);
                    myTurn = false;
                    roomPanel.resetToChatLayout();
                    roomPanel.setTurnText("Chờ người chơi...");
                    cardLayout.show(mainPanel, "room");
                    break;
                case "ROOM_JOINED":
                    if (msg.fieldCount() >= 2) {
                        currentRoom = msg.field(0);
                        if (!isOwner) {
                            roomPanel.setRoomInfo("Phòng: " + currentRoom);
                            roomPanel.clearChat();
                            roomPanel.appendChat("\uD83D\uDEAA Bạn đã vào phòng " + currentRoom + "\n");
                            roomPanel.appendChat("\uD83D\uDC65 Người chơi: " + msg.field(1) + "\n");
                            roomPanel.setStartEnabled(false);
                            roomPanel.setSecretEnabled(false);
                            roomPanel.setGuessEnabled(false);
                            myTurn = false;
                            roomPanel.resetToChatLayout();
                            roomPanel.setTurnText("Chờ bắt đầu...");
                            cardLayout.show(mainPanel, "room");
                        }
                    }
                    break;
                case "ROOM_PLAYER_JOINED":
                    if (msg.fieldCount() >= 1)
                        roomPanel.appendChat("\u27A1\uFE0F " + msg.field(0) + " đã vào phòng\n");
                    break;
                case "ROOM_PLAYER_LEFT":
                    if (msg.fieldCount() >= 1)
                        roomPanel.appendChat("\u2B05\uFE0F " + msg.field(0) + " đã rời phòng\n");
                    break;
                case "ROOM_CHAT_MSG":
                    if (msg.fieldCount() >= 2)
                        roomPanel.appendChat(msg.field(0) + ": " + msg.field(1) + "\n");
                    break;
                case "ROOM_LEFT":
                    stopLocalTimer();
                    currentRoom = null;
                    isOwner = false;
                    myTurn = false;
                    cardLayout.show(mainPanel, "lobby");
                    break;
                case "GAME_START":
                    if (msg.fieldCount() >= 1)
                        numDigits = Integer.parseInt(msg.field(0));
                    roomPanel.showGameLayout(numDigits);
                    roomPanel.appendChat("\uD83C\uDFAE GAME BẮT ĐẦU! Chọn số bí mật " + numDigits + " chữ số.\n");
                    roomPanel.setStartEnabled(false);
                    roomPanel.setSecretEnabled(true);
                    roomPanel.setGuessEnabled(false);
                    roomPanel.setChatEnabled(false);
                    roomPanel.setTurnText("Đặt số bí mật!");
                    break;
                case "SECRET_SET":
                    roomPanel.setSecretEnabled(false);
                    roomPanel.setSecretEditable(false);
                    roomPanel.appendChat("\u2705 Số bí mật của bạn: " + msg.field(0) + "\n");
                    roomPanel.setTurnText("Chờ người khác...");
                    break;
                case "YOUR_TURN":
                    myTurn = true;
                    roomPanel.setGuessEnabled(true);
                    roomPanel.setGuessEditable(true);
                    roomPanel.focusGuess();
                    String target = msg.field(0).isEmpty() ? "?" : msg.field(0);
                    timeLeft = msg.fieldCount() >= 2 ? Integer.parseInt(msg.field(1)) : 20;
                    roomPanel.setTurnText("\uD83C\uDFAF Lượt bạn! (" + timeLeft + "s) Đoán số của " + target);
                    roomPanel.appendChat(
                            "\uD83C\uDFAF ĐẾN LƯỢT BẠN! Đoán số của " + target + " (Có " + timeLeft + "s)\n");

                    stopLocalTimer();
                    turnTimer = new javax.swing.Timer(1000, e -> {
                        timeLeft--;
                        if (timeLeft >= 0) {
                            roomPanel.setTurnText("\uD83C\uDFAF Lượt bạn! (" + timeLeft + "s) Đoán số của " + target);
                        } else {
                            stopLocalTimer();
                        }
                    });
                    turnTimer.start();
                    break;
                case "TURN_TIMEOUT":
                    stopLocalTimer();
                    myTurn = false;
                    roomPanel.setGuessEnabled(false);
                    roomPanel.setTurnText("Hết giờ! Chờ lượt...");
                    break;
                case "GUESS_RESULT":
                    stopLocalTimer();
                    if (msg.fieldCount() >= 1) {
                        String correct = msg.field(0);
                        roomPanel.appendChat("\uD83D\uDCCA Kết quả: " + correct + "/" + numDigits + " số đúng\n");
                        roomPanel.addHistory(lastGuess, correct, "0"); // Fake "Bê" = 0 for now
                        if (!correct.equals(String.valueOf(numDigits))) {
                            myTurn = false;
                            roomPanel.setGuessEnabled(false);
                            roomPanel.setTurnText("Chờ lượt...");
                        }
                    }
                    break;
                case "PLAYER_WON":
                    if (msg.fieldCount() >= 2) {
                        String winner = msg.field(0);
                        String rank = msg.field(1);
                        roomPanel.appendChat("🏆 " + winner + " thắng! Hạng #" + rank + "\n");
                        if (winner.equals(nickname)) {
                            stopLocalTimer();
                            myTurn = false;
                            roomPanel.setGuessEnabled(false);
                            roomPanel.setGuessEditable(false);
                            roomPanel.resetToChatLayout();
                            roomPanel.setTurnText("🏆 Hạng #" + rank + " - Đang xem...");
                            roomPanel.appendChat(
                                    "\n🎉 Chúc mừng! Bạn hoàn thành nhiệm vụ.\n   Hãy xem tiếp hoặc bấm [ Rời ] để thoát.\n");
                        }
                    }
                    break;
                case "GAME_OVER":
                    stopLocalTimer();
                    roomPanel.appendChat("\n\uD83C\uDF8A GAME KẾT THÚC!\n");
                    if (msg.fieldCount() >= 1) {
                        String[] ranks = msg.field(0).split(",");
                        for (int i = 0; i < ranks.length; i++) {
                            String m = i == 0 ? "\uD83E\uDD47"
                                    : i == 1 ? "\uD83E\uDD48" : i == 2 ? "\uD83E\uDD49" : "  ";
                            roomPanel.appendChat(m + " #" + (i + 1) + " " + ranks[i] + "\n");
                        }
                    }
                    roomPanel.setTurnText("Game kết thúc!");
                    roomPanel.setGuessEnabled(false);
                    roomPanel.setChatEnabled(true);
                    myTurn = false;
                    break;
                case "ERROR":
                    String errMsg = msg.field(0).isEmpty() ? "Lỗi không xác định" : msg.field(0);
                    JOptionPane.showMessageDialog(this, errMsg, "Lỗi", JOptionPane.WARNING_MESSAGE);
                    break;
                case "SERVER_SHUTDOWN":
                    connected = false;
                    JOptionPane.showMessageDialog(this, "Server đã tắt!");
                    cardLayout.show(mainPanel, "login");
                    break;
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient().setVisible(true));
    }
}
