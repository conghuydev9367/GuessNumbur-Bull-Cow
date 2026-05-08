package com.guessnumber.server;

import com.guessnumber.common.Message;
import com.guessnumber.theme.NordTheme;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer extends JFrame {

    private static final int TCP_PORT = 9876;
    private static final int UDP_PORT = 9877;
    private static final String DISCOVER_REQ = "GAME_DISCOVER";
    private static final String DISCOVER_RES = "GAME_SERVER";

    private JTextArea logArea;
    private JTextArea chatArea;
    private JTextField chatField;
    private JLabel statusLabel, clientCountLabel;

    private ServerSocket serverSocket;
    private DatagramSocket udpSocket;
    private volatile boolean running = true;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final List<String> chatHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    public GameServer() {
        setupGUI();
        startServer();
    }

    private void setupGUI() {
        setTitle("🎮 Game Đoán Số - Server");
        setSize(750, 600);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(NordTheme.BG);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(NordTheme.FROST3);
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        statusLabel = NordTheme.label("⏳ Đang khởi động...", Color.WHITE);
        clientCountLabel = NordTheme.label("Clients: 0", NordTheme.SNOW0);
        topPanel.add(statusLabel, BorderLayout.WEST);
        topPanel.add(clientCountLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(NordTheme.BG);
        splitPane.setDividerLocation(370);
        splitPane.setBorder(null);

        JPanel logPanel = createPanel("📋 Server Log");
        logArea = NordTheme.textArea();
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logPanel.add(logScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(logPanel);

        JPanel chatPanel = createPanel("💬 Lobby Chat");
        chatArea = NordTheme.textArea();
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(null);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        chatInputPanel.setBackground(NordTheme.BG_SEC);
        chatInputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        chatField = NordTheme.textField("");
        chatField.addActionListener(e -> sendServerChat());
        JButton sendBtn = NordTheme.button("Gửi", NordTheme.FROST3);
        sendBtn.addActionListener(e -> sendServerChat());
        chatInputPanel.add(chatField, BorderLayout.CENTER);
        chatInputPanel.add(sendBtn, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(chatPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createPanel(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NordTheme.BG_SEC);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(NordTheme.BORDER),
                        title, TitledBorder.LEFT, TitledBorder.TOP,
                        null, NordTheme.FROST3)));
        return p;
    }

    private void startServer() {
        new Thread(this::runTCPServer, "TCP-Server").start();
        new Thread(this::runUDPDiscovery, "UDP-Discovery").start();
    }

    private void runTCPServer() {
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            InetAddress addr = InetAddress.getLocalHost();
            log("✅ TCP Server đã khởi động tại port " + TCP_PORT);

            // Liệt kê tất cả IP của máy (Ethernet, WiFi, ...)
            log("─────────────────────────────");
            java.util.Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                java.util.Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address) {
                        log("📡 " + iface.getDisplayName() + ": " + a.getHostAddress());
                    }
                }
            }
            log("─────────────────────────────");
            log("💡 Dùng IP phù hợp với interface đang kết nối cùng Client");

            SwingUtilities.invokeLater(() ->
                    statusLabel.setText("🟢 Đang chạy - " + addr.getHostAddress() + ":" + TCP_PORT));

            while (running) {
                Socket sock = serverSocket.accept();
                ClientHandler ch = new ClientHandler(sock);
                clients.add(ch);
                ch.start();
                log("🔗 Kết nối mới: " + sock.getInetAddress().getHostAddress());
                updateClientCount();
            }
        } catch (IOException e) {
            if (running)
                log("❌ TCP Error: " + e.getMessage());
        }
    }


    private void runUDPDiscovery() {
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
            udpSocket.setBroadcast(true);
            log("✅ UDP Discovery đang lắng nghe tại port " + UDP_PORT);
            byte[] buf = new byte[256];

            while (running) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                udpSocket.receive(pkt);
                String msg = new String(pkt.getData(), 0, pkt.getLength());

                if (msg.equals(DISCOVER_REQ)) {
                    InetAddress clientAddr = pkt.getAddress();
                    int clientPort = pkt.getPort();
                    Message resp = Message.of(DISCOVER_RES, String.valueOf(TCP_PORT),
                            InetAddress.getLocalHost().getHostAddress());
                    byte[] respData = resp.serialize().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    udpSocket.send(new DatagramPacket(respData, respData.length, clientAddr, clientPort));
                    log("📡 Discovery từ " + clientAddr.getHostAddress());
                }
            }
        } catch (IOException e) {
            if (running)
                log("❌ UDP Error: " + e.getMessage());
        }
    }

    private void sendServerChat() {
        String msg = chatField.getText().trim();
        if (msg.isEmpty())
            return;
        chatField.setText("");
        broadcastToAll(Message.of("CHAT_MSG", "🖥️ Server", msg));
        appendChat("🖥️ Server: " + msg);
    }

    private void broadcastToAll(Message msg) {
        synchronized (chatHistory) {
            chatHistory.add(msg.serialize());
            if (chatHistory.size() > MAX_HISTORY) {
                chatHistory.remove(0);
            }
        }
        for (ClientHandler ch : clients) {
            if (ch.nickname != null)
                ch.send(msg);
        }
    }

    private void sendChatHistory(ClientHandler client) {
        synchronized (chatHistory) {
            for (String serialized : chatHistory) {
                client.send(Message.parse(serialized));
            }
        }

        for (Map.Entry<String, Room> entry : rooms.entrySet()) {
            Room room = entry.getValue();
            if (!room.gameStarted) {
                client.send(Message.of("ROOM_ANNOUNCE", room.code, room.owner, String.valueOf(room.numDigits),
                        String.valueOf(room.turnTime)));
            }
        }
    }

    private void broadcastToRoom(String roomCode, Message msg) {
        Room room = rooms.get(roomCode);
        if (room == null)
            return;
        for (String player : room.players) {
            ClientHandler ch = findClient(player);
            if (ch != null)
                ch.send(msg);
        }
    }

    private ClientHandler findClient(String nickname) {
        for (ClientHandler ch : clients) {
            if (nickname.equals(ch.nickname))
                return ch;
        }
        return null;
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rng = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++)
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        String code = sb.toString();
        return rooms.containsKey(code) ? generateRoomCode() : code;
    }

    private synchronized void handleCreateRoom(ClientHandler client, int numDigits, int turnTime) {
        if (client.currentRoom != null) {
            client.send(Message.of("ERROR", "Bạn đang ở trong phòng rồi!"));
            return;
        }
        if (numDigits < 2 || numDigits > 10) {
            client.send(Message.of("ERROR", "Số chữ số phải từ 2 đến 10!"));
            return;
        }
        if (turnTime < 10 || turnTime > 40) {
            client.send(Message.of("ERROR", "Thời gian lượt phải từ 10s đến 40s!"));
            return;
        }
        String code = generateRoomCode();
        Room room = new Room(code, client.nickname, numDigits, turnTime);
        room.players.add(client.nickname);
        rooms.put(code, room);
        client.currentRoom = code;

        client.send(Message.of("ROOM_CREATED", code));
        client.send(Message.of("ROOM_JOINED", code, client.nickname));
        broadcastToAll(Message.of("ROOM_ANNOUNCE", code, client.nickname, String.valueOf(numDigits),
                String.valueOf(turnTime)));
        appendChat("🏠 " + client.nickname + " tạo phòng " + code);
        log("🏠 Phòng " + code + " được tạo bởi " + client.nickname);
    }

    private synchronized void handleJoinRoom(ClientHandler client, String code) {
        if (client.currentRoom != null) {
            client.send(Message.of("ERROR", "Bạn đang ở trong phòng rồi!"));
            return;
        }
        Room room = rooms.get(code);
        if (room == null) {
            client.send(Message.of("ERROR", "Không tìm thấy phòng " + code + "!"));
            return;
        }
        if (room.gameStarted) {
            client.send(Message.of("ERROR", "Phòng đang chơi, không thể vào!"));
            return;
        }
        if (room.players.size() >= 6) {
            client.send(Message.of("ERROR", "Phòng đã đầy (6/6)!"));
            return;
        }

        room.players.add(client.nickname);
        client.currentRoom = code;

        String playerList = String.join(",", room.players);
        client.send(Message.of("ROOM_JOINED", code, playerList));
        broadcastToRoom(code, Message.of("ROOM_PLAYER_JOINED", client.nickname, String.valueOf(room.players.size())));
        broadcastToAll(Message.of("CHAT_MSG", "🎮 Hệ thống", client.nickname + " đã vào phòng [" + code + "]"));
        log("👤 " + client.nickname + " vào phòng " + code);
    }

    private synchronized void handleLeaveRoom(ClientHandler client) {
        if (client.currentRoom == null)
            return;
        Room room = rooms.get(client.currentRoom);
        if (room == null) {
            client.currentRoom = null;
            return;
        }

        room.players.remove(client.nickname);
        boolean wasGuesser = false;
        if (room.gameStarted) {
            if (!room.activePlayers.isEmpty()) {
                wasGuesser = room.activePlayers.get(room.currentTurnIndex % room.activePlayers.size())
                        .equals(client.nickname);
            }
            room.activePlayers.remove(client.nickname);
            room.secrets.remove(client.nickname);
            room.rankings.add(client.nickname);
            if (wasGuesser && room.turnTimer != null) {
                room.turnTimer.cancel();
                room.turnTimer = null;
            }
        }

        broadcastToRoom(client.currentRoom, Message.of("ROOM_PLAYER_LEFT", client.nickname));
        log("👤 " + client.nickname + " rời phòng " + client.currentRoom);

        if (client.nickname.equals(room.owner)) {
            if (room.players.isEmpty()) {
                rooms.remove(client.currentRoom);
                log("🗑️ Phòng " + client.currentRoom + " đã bị xóa");
            } else {
                room.owner = room.players.get(0);
                broadcastToRoom(client.currentRoom,
                        Message.of("ROOM_CHAT_MSG", "🎮 Hệ thống", room.owner + " là chủ phòng mới"));
            }
        }

        client.send(Message.of("ROOM_LEFT"));
        String prevRoom = client.currentRoom;
        client.currentRoom = null;

        if (room.gameStarted) {
            if (room.activePlayers.size() <= 1) {
                endGame(room, prevRoom);
            } else if (wasGuesser) {
                room.currentTurnIndex = room.currentTurnIndex % room.activePlayers.size();
                startTurn(room, prevRoom);
            }
        }
    }

    private synchronized void handleStartGame(ClientHandler client) {
        if (client.currentRoom == null)
            return;
        Room room = rooms.get(client.currentRoom);
        if (room == null)
            return;

        if (!client.nickname.equals(room.owner)) {
            client.send(Message.of("ERROR", "Chỉ chủ phòng mới có thể bắt đầu!"));
            return;
        }
        if (room.players.size() < 2) {
            client.send(Message.of("ERROR", "Cần ít nhất 2 người chơi!"));
            return;
        }
        if (room.gameStarted) {
            client.send(Message.of("ERROR", "Game đã bắt đầu rồi!"));
            return;
        }

        room.gameStarted = true;
        room.activePlayers = new ArrayList<>(room.players);
        room.secrets = new HashMap<>();
        room.rankings = new ArrayList<>();
        room.currentTurnIndex = 0;
        room.secretsReady = 0;

        broadcastToRoom(client.currentRoom, Message.of("GAME_START", String.valueOf(room.numDigits)));
        broadcastToRoom(client.currentRoom, Message.of("ROOM_CHAT_MSG", "🎮 Hệ thống",
                "Game bắt đầu! Hãy chọn số bí mật " + room.numDigits + " chữ số."));
        log("🎮 Game bắt đầu tại phòng " + client.currentRoom);
    }

    private synchronized void handleSetSecret(ClientHandler client, String secret) {
        if (client.currentRoom == null)
            return;
        Room room = rooms.get(client.currentRoom);
        if (room == null || !room.gameStarted)
            return;

        if (secret.length() != room.numDigits || !secret.matches("\\d+")) {
            client.send(Message.of("ERROR", "Số bí mật phải có đúng " + room.numDigits + " chữ số!"));
            return;
        }

        room.secrets.put(client.nickname, secret);
        room.secretsReady++;
        client.send(Message.of("SECRET_SET", secret));
        broadcastToRoom(client.currentRoom,
                Message.of("ROOM_CHAT_MSG", "🎮 Hệ thống", client.nickname + " đã chọn số bí mật ✅"));

        if (room.secretsReady == room.activePlayers.size()) {
            broadcastToRoom(client.currentRoom,
                    Message.of("ROOM_CHAT_MSG", "🎮 Hệ thống", "Tất cả đã sẵn sàng! Bắt đầu đoán số!"));
            startTurn(room, client.currentRoom);
        }
    }

    private void startTurn(Room room, String roomCode) {
        if (room.activePlayers.size() <= 1) {
            endGame(room, roomCode);
            return;
        }

        int idx = room.currentTurnIndex % room.activePlayers.size();
        String guesser = room.activePlayers.get(idx);
        int targetIdx = (idx + 1) % room.activePlayers.size();
        String target = room.activePlayers.get(targetIdx);

        ClientHandler guesserClient = findClient(guesser);
        if (guesserClient != null)
            guesserClient.send(Message.of("YOUR_TURN", target, String.valueOf(room.turnTime)));
        broadcastToRoom(roomCode,
                Message.of("ROOM_CHAT_MSG", "🎮 Hệ thống", "Lượt của " + guesser + " → đoán số của " + target));

        if (room.turnTimer != null) {
            room.turnTimer.cancel();
        }
        room.turnTimer = new java.util.Timer();
        room.turnTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                handleTimeout(room, roomCode, guesser);
            }
        }, room.turnTime * 1000L);
    }

    private synchronized void handleTimeout(Room room, String roomCode, String expectedGuesser) {
        if (room == null || !room.gameStarted || room.activePlayers.isEmpty())
            return;
        int idx = room.currentTurnIndex % room.activePlayers.size();
        String currentGuesser = room.activePlayers.get(idx);
        if (!currentGuesser.equals(expectedGuesser))
            return;

        broadcastToRoom(roomCode, Message.of("ROOM_CHAT_MSG", "⏰",
                currentGuesser + " đã hết thời gian (" + room.turnTime + "s) và mất lượt!"));
        ClientHandler guesserClient = findClient(currentGuesser);
        if (guesserClient != null)
            guesserClient.send(Message.of("TURN_TIMEOUT"));

        room.currentTurnIndex = (idx + 1) % room.activePlayers.size();
        startTurn(room, roomCode);
    }

    private synchronized void handleGuess(ClientHandler client, String guess) {
        if (client.currentRoom == null)
            return;
        Room room = rooms.get(client.currentRoom);
        if (room == null || !room.gameStarted)
            return;

        int idx = room.currentTurnIndex % room.activePlayers.size();
        String currentGuesser = room.activePlayers.get(idx);
        if (!client.nickname.equals(currentGuesser)) {
            client.send(Message.of("ERROR", "Chưa đến lượt của bạn!"));
            return;
        }
        if (guess.length() != room.numDigits || !guess.matches("\\d+")) {
            client.send(Message.of("ERROR", "Số đoán phải có đúng " + room.numDigits + " chữ số!"));
            return;
        }

        if (room.turnTimer != null) {
            room.turnTimer.cancel();
            room.turnTimer = null;
        }

        int targetIdx = (idx + 1) % room.activePlayers.size();
        String target = room.activePlayers.get(targetIdx);
        String secret = room.secrets.get(target);

        int correctCount = 0;
        for (int i = 0; i < secret.length(); i++) {
            if (secret.charAt(i) == guess.charAt(i))
                correctCount++;
        }

        client.send(Message.of("GUESS_RESULT", String.valueOf(correctCount)));

        // Broadcast công khai — không lộ bí mật
        broadcastToRoom(client.currentRoom, Message.of("ROOM_CHAT_MSG", "🎮",
                client.nickname + " đoán [" + guess + "] → " + correctCount + "/" + room.numDigits + " số đúng"));

        // Gửi riêng cho Target: thấy được số đoán so với bí mật của mình
        ClientHandler targetClient = findClient(target);
        if (targetClient != null) {
            targetClient.send(Message.of("ROOM_CHAT_MSG", "🎯 Riêng bạn",
                    "[" + guess + "] so với số của bạn [" + secret + "] → " + correctCount + "/" + room.numDigits
                            + " đúng"));
        }

        // Gửi riêng cho những người đã thắng (spectator) để theo dõi
        for (String rankedPlayer : room.rankings) {
            ClientHandler rch = findClient(rankedPlayer);
            if (rch != null && !rankedPlayer.equals(target)) {
                rch.send(Message.of("ROOM_CHAT_MSG", "👁️ Khán giả",
                        client.nickname + " đoán [" + guess + "] | bí mật " + target + ": [" + secret + "] → "
                                + correctCount + "/" + room.numDigits));
            }
        }

        if (correctCount == room.numDigits) {
            int rank = room.rankings.size() + 1;
            room.rankings.add(client.nickname);
            broadcastToRoom(client.currentRoom, Message.of("PLAYER_WON", client.nickname, String.valueOf(rank)));
            broadcastToRoom(client.currentRoom, Message.of("ROOM_CHAT_MSG", "🏆", client.nickname
                    + " đã đoán trúng số của " + target + " [" + secret + "]! Xếp hạng #" + rank
                    + "\n  → " + client.nickname + " thoát khỏi trận. " + target + " tiếp tục chơi!"));

            int winnerIdx = room.activePlayers.indexOf(client.nickname);
            room.activePlayers.remove(client.nickname);

            if (room.activePlayers.size() <= 1) {
                if (room.activePlayers.size() == 1)
                    room.rankings.add(room.activePlayers.get(0));
                endGame(room, client.currentRoom);
            } else {
                // Dùng < (không phải <=) để tránh currentTurnIndex bị âm trong Java
                // Khi attacker thắng (winnerIdx == currentTurnIndex), không giảm index:
                // phần tử tại index đó giờ là target (B) → B tiếp tục là người đoán tiếp
                if (winnerIdx < room.currentTurnIndex)
                    room.currentTurnIndex--;
                room.currentTurnIndex = room.currentTurnIndex % room.activePlayers.size();
                startTurn(room, client.currentRoom);
            }
        } else {
            room.currentTurnIndex = (idx + 1) % room.activePlayers.size();
            startTurn(room, client.currentRoom);
        }
    }

    private void endGame(Room room, String roomCode) {
        if (room.turnTimer != null) {
            room.turnTimer.cancel();
            room.turnTimer = null;
        }

        for (String p : room.activePlayers) {
            if (!room.rankings.contains(p)) {
                room.rankings.add(p);
            }
        }
        room.activePlayers.clear();

        StringBuilder sb = new StringBuilder();
        sb.append("\n══════ 🏆 BẢNG XẾP HẠNG 🏆 ══════\n");
        for (int i = 0; i < room.rankings.size(); i++) {
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "  ";
            String playerName = room.rankings.get(i);
            String playerSecret = room.secrets.getOrDefault(playerName, "???");
            sb.append(medal).append(" #").append(i + 1).append(" - ")
                    .append(playerName).append(" (số bí mật: ").append(playerSecret).append(")\n");
        }
        sb.append("══════════════════════════════\n");

        broadcastToRoom(roomCode, Message.of("ROOM_CHAT_MSG", "🎮 Hệ thống", sb.toString()));
        broadcastToRoom(roomCode, Message.of("GAME_OVER", String.join(",", room.rankings)));
        broadcastToAll(Message.of("CHAT_MSG", "🎮 Hệ thống",
                "Phòng [" + roomCode + "] đã kết thúc game! 🏆 " + room.rankings.get(0)));
        appendChat("🏆 Phòng " + roomCode + " kết thúc - Thắng: " + room.rankings.get(0));
        log("🏆 Game kết thúc tại phòng " + roomCode);

        for (String player : new ArrayList<>(room.players)) {
            ClientHandler ch = findClient(player);
            if (ch != null) {
                ch.send(Message.of("ROOM_LEFT"));
                ch.currentRoom = null;
            }
        }
        rooms.remove(roomCode);
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void appendChat(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateClientCount() {
        long count = clients.stream().filter(c -> c.nickname != null).count();
        SwingUtilities.invokeLater(() -> clientCountLabel.setText("Clients: " + count));
    }

    private void shutdown() {
        running = false;
        broadcastToAll(Message.of("SERVER_SHUTDOWN"));
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (Exception ignored) {
        }
        try {
            if (udpSocket != null)
                udpSocket.close();
        } catch (Exception ignored) {
        }
        for (ClientHandler ch : clients)
            ch.close();
        System.exit(0);
    }

    class ClientHandler extends Thread {
        Socket link;
        PrintWriter output;
        Scanner input;
        String nickname;
        String currentRoom;

        ClientHandler(Socket link) {
            this.link = link;
            setDaemon(true);
        }

        public void run() {
            try {
                input = new Scanner(link.getInputStream());
                output = new PrintWriter(link.getOutputStream(), true);
                send(Message.of("WELCOME", "Chào mừng đến Game Đoán Số! Hãy đặt nickname."));

                while (running && input.hasNextLine()) {
                    String line = input.nextLine();
                    processMessage(line);
                }
            } catch (IOException ignored) {
            } finally {
                handleDisconnect();
            }
        }

        private void processMessage(String line) {
            Message msg = Message.parse(line);
            String cmd = msg.command();
            String data = msg.field(0);

            switch (cmd) {
                case "NICK":
                    handleNick(data);
                    break;
                case "CHAT":
                    if (nickname != null) {
                        if (currentRoom != null) {
                            Room r = rooms.get(currentRoom);
                            if (r != null && r.gameStarted) {
                                send(Message.of("ERROR", "Không thể chat lobby khi đang chơi!"));
                                break;
                            }
                        }
                        broadcastToAll(Message.of("CHAT_MSG", nickname, data));
                        appendChat(nickname + ": " + data);
                    }
                    break;
                case "CREATE_ROOM":
                    try {
                        int digits = Integer.parseInt(msg.field(0).trim());
                        int turnTime = msg.fieldCount() > 1 ? Integer.parseInt(msg.field(1).trim()) : 20;
                        handleCreateRoom(this, digits, turnTime);
                    } catch (NumberFormatException e) {
                        send(Message.of("ERROR", "Tham số không hợp lệ!"));
                    }
                    break;
                case "JOIN_ROOM":
                    handleJoinRoom(this, data.trim().toUpperCase());
                    break;
                case "LEAVE_ROOM":
                    handleLeaveRoom(this);
                    break;
                case "ROOM_CHAT":
                    if (currentRoom != null && nickname != null) {
                        Room r = rooms.get(currentRoom);
                        if (r != null && r.gameStarted) {
                            send(Message.of("ERROR", "Không thể chat khi đang chơi!"));
                            break;
                        }
                        broadcastToRoom(currentRoom, Message.of("ROOM_CHAT_MSG", nickname, data));
                    }
                    break;
                case "START_GAME":
                    handleStartGame(this);
                    break;
                case "SET_SECRET":
                    handleSetSecret(this, data.trim());
                    break;
                case "GUESS":
                    handleGuess(this, data.trim());
                    break;
            }
        }

        private void handleNick(String name) {
            name = name.trim();
            if (name.isEmpty() || name.length() > 20) {
                send(Message.of("ERROR", "Nickname không hợp lệ (1-20 ký tự)!"));
                return;
            }
            for (ClientHandler ch : clients) {
                if (name.equals(ch.nickname)) {
                    send(Message.of("ERROR", "Nickname đã được sử dụng!"));
                    return;
                }
            }
            this.nickname = name;
            send(Message.of("NICK_OK", name));

            sendChatHistory(this);
            broadcastToAll(Message.of("CHAT_MSG", "🎮 Hệ thống", name + " đã tham gia lobby!"));
            appendChat("➡️ " + name + " đã tham gia");
            log("👤 " + name + " đã kết nối (" + link.getInetAddress().getHostAddress() + ")");
            updateClientCount();
        }

        private void handleDisconnect() {
            if (currentRoom != null)
                handleLeaveRoom(this);
            clients.remove(this);
            if (nickname != null) {
                broadcastToAll(Message.of("CHAT_MSG", "🎮 Hệ thống", nickname + " đã rời đi."));
                appendChat("⬅️ " + nickname + " đã rời đi");
                log("👤 " + nickname + " đã ngắt kết nối");
            }
            updateClientCount();
            close();
        }

        void send(Message msg) {
            if (output != null && msg != null)
                output.println(msg.serialize());
        }

        void close() {
            try {
                if (link != null)
                    link.close();
            } catch (Exception ignored) {
            }
        }
    }

    static class Room {
        String code, owner;
        int numDigits;
        int turnTime;
        java.util.Timer turnTimer;
        List<String> players = new ArrayList<>();
        boolean gameStarted = false;
        Map<String, String> secrets = new HashMap<>();
        List<String> activePlayers = new ArrayList<>();
        List<String> rankings = new ArrayList<>();
        int currentTurnIndex = 0;
        int secretsReady = 0;

        Room(String code, String owner, int numDigits, int turnTime) {
            this.code = code;
            this.owner = owner;
            this.numDigits = numDigits;
            this.turnTime = turnTime;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameServer().setVisible(true));
    }
}
