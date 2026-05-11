package com.mycompany.networkproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameFrame extends JFrame {
    private final Client client;
    private final PieceColor role;
    private final JLabel statusLabel = new JLabel("Connecting...", SwingConstants.CENTER);
    private final BoardPanel board;
    private final JButton resignButton = new JButton("Resign");
    private final JButton offerDrawButton = new JButton("Offer Draw");
    private final JButton acceptDrawButton = new JButton("Accept Draw");
    private final JButton declineDrawButton = new JButton("Decline Draw");
    private boolean endScreenShown = false;

    public GameFrame(Client client, PieceColor role) {
        this.client = client;
        this.role = role;
        setTitle("Chess - " + role);
        setSize(760, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.exitApplication();
            }
        });
        setLayout(new BorderLayout(8, 8));

        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(statusLabel, BorderLayout.NORTH);

        board = new BoardPanel(client, role);
        add(board, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        actions.add(resignButton);
        actions.add(offerDrawButton);
        actions.add(acceptDrawButton);
        actions.add(declineDrawButton);
        add(actions, BorderLayout.SOUTH);

        resignButton.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Resign the game?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) client.resign();
        });
        offerDrawButton.addActionListener(e -> client.offerDraw());
        acceptDrawButton.addActionListener(e -> client.acceptDraw());
        declineDrawButton.addActionListener(e -> client.declineDraw());

        setVisible(true);
    }

    public void receiveState(String stateMessage) {
        String[] p = stateMessage.split("\\|", 8);
        if (p.length < 8) throw new IllegalArgumentException("Incomplete STATE message");
        PieceColor turn = PieceColor.valueOf(p[1]);
        GameStatus status = GameStatus.valueOf(p[2]);
        String winner = p[3];
        String message = p[4];
        String boardEncoded = p[7];
        Piece[][] boardState = GameState.deserializeBoard(boardEncoded);

        boolean myTurn = status == GameStatus.ACTIVE || status == GameStatus.CHECK;
        myTurn = myTurn && turn == role;
        statusLabel.setText("You are " + role + " | " + (myTurn ? "Your turn" : "Opponent's turn") + " | " + message);
        board.setState(boardState, turn, status, message);
        updateActionButtons(status);

        if (isGameOver(status) && !endScreenShown) {
            endScreenShown = true;
            String result = winner.equals("NONE") ? message : message + " Winner: " + winner;
            new EndScreen(result, client, this);
        }
    }

    private boolean isGameOver(GameStatus status) {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE || status == GameStatus.DRAW
                || status == GameStatus.RESIGNED || status == GameStatus.DISCONNECTED;
    }

    private void updateActionButtons(GameStatus status) {
        boolean active = !isGameOver(status);
        resignButton.setEnabled(active);
        offerDrawButton.setEnabled(active);
        acceptDrawButton.setEnabled(active);
        declineDrawButton.setEnabled(active);
    }

    public void showConnectionError(String message) {
        statusLabel.setText(message);
        board.setInteractionEnabled(false);
        updateActionButtons(GameStatus.DISCONNECTED);
        JOptionPane.showMessageDialog(this, message, "Connection", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isEndScreenShown() { return endScreenShown; }

    public void closeFrame() { dispose(); }

    public void exitApplication() { client.exitApplication(); }
}
