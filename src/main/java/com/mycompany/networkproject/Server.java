package com.mycompany.networkproject;

import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT + "...");
            while (true) {
                System.out.println("Waiting for players...");
                Socket player1 = serverSocket.accept();
                System.out.println("White connected: " + player1.getRemoteSocketAddress());
                Socket player2 = serverSocket.accept();
                System.out.println("Black connected: " + player2.getRemoteSocketAddress());
                Thread session = new Thread(new GameSession(player1, player2), "game-session");
                session.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class GameSession implements Runnable {
        private final PlayerConnection white;
        private final PlayerConnection black;
        private final GameState game = new GameState();
        private volatile boolean running = true;

        GameSession(Socket whiteSocket, Socket blackSocket) throws IOException {
            white = new PlayerConnection(whiteSocket, PieceColor.WHITE);
            black = new PlayerConnection(blackSocket, PieceColor.BLACK);
        }

        @Override
        public void run() {
            try {
                white.send("ROLE|WHITE");
                black.send("ROLE|BLACK");
                broadcastState();

                Thread t1 = new Thread(() -> listen(white), "white-listener");
                Thread t2 = new Thread(() -> listen(black), "black-listener");
                t1.start();
                t2.start();
                t1.join();
                t2.join();
            } catch (Exception e) {
                System.err.println("Game session error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                closeQuietly(white);
                closeQuietly(black);
                System.out.println("Game session closed.");
            }
        }

        private void listen(PlayerConnection player) {
            try {
                while (running) {
                    String message = player.in.readUTF();
                    handleMessage(player, message);
                }
            } catch (EOFException | SocketException e) {
                handleDisconnect(player);
            } catch (Exception e) {
                System.err.println(player.color + " listener error: " + e.getMessage());
                e.printStackTrace();
                handleDisconnect(player);
            }
        }

        private synchronized void handleMessage(PlayerConnection player, String message) {
            try {
                String[] parts = message.split("\\|");
                switch (parts[0]) {
                    case "MOVE" -> {
                        Move move = Move.fromProtocol(parts);
                        MoveResult result = game.playMove(move, player.color);
                        if (!result.success) {
                            player.send("ERROR|" + GameState.escape(result.message));
                            return;
                        }
                        broadcastState();
                    }
                    case "RESIGN" -> {
                        game.resign(player.color);
                        broadcastState();
                        finishSession();
                    }
                    case "DRAW_OFFER" -> {
                        String result = game.offerDraw(player.color);
                        if ("Game is already over".equals(result)) {
                            player.send("ERROR|" + GameState.escape(result));
                            return;
                        }
                        broadcastState();
                    }
                    case "DRAW_ACCEPT" -> {
                        String result = game.respondDraw(player.color, true);
                        if ("There is no active draw offer.".equals(result) || "You cannot answer your own draw offer.".equals(result)) {
                            player.send("ERROR|" + GameState.escape(result));
                            return;
                        }
                        broadcastState();
                        if (game.isGameOver()) finishSession();
                    }
                    case "DRAW_DECLINE" -> {
                        String result = game.respondDraw(player.color, false);
                        if ("There is no active draw offer.".equals(result) || "You cannot answer your own draw offer.".equals(result)) {
                            player.send("ERROR|" + GameState.escape(result));
                            return;
                        }
                        broadcastState();
                    }
                    default -> player.send("ERROR|Unknown command: " + GameState.escape(parts[0]));
                }
                if (game.isGameOver()) finishSession();
            } catch (Exception e) {
                player.send("ERROR|Invalid message: " + GameState.escape(e.getMessage()));
                System.err.println("Invalid client message from " + player.color + ": " + message);
                e.printStackTrace();
            }
        }

        private synchronized void handleDisconnect(PlayerConnection disconnected) {
            if (!running && game.isGameOver()) return;
            System.out.println(disconnected.color + " disconnected.");
            game.opponentDisconnected(disconnected.color);
            broadcastState();
            finishSession();
        }

        private synchronized void finishSession() {
            running = false;
            closeQuietly(white);
            closeQuietly(black);
        }

        private void broadcastState() {
            String state = game.toStateMessage();
            white.send(state);
            black.send(state);
        }

        private void closeQuietly(PlayerConnection p) {
            try { p.socket.close(); } catch (Exception ignored) { }
        }
    }

    private static class PlayerConnection {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final PieceColor color;

        PlayerConnection(Socket socket, PieceColor color) throws IOException {
            this.socket = socket;
            this.color = color;
            this.socket.setKeepAlive(true);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        synchronized void send(String message) {
            try {
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("Send failed to " + color + ": " + e.getMessage());
            }
        }
    }
}
