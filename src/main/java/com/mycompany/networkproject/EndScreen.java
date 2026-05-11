package com.mycompany.networkproject;

import javax.swing.*;
import java.awt.*;

public class EndScreen extends JFrame {
    public EndScreen(String result, Client client, GameFrame frame) {
        setTitle("Game Over");
        setSize(420, 220);
        setLocationRelativeTo(frame);
        setLayout(new BorderLayout(10, 10));

        JLabel label = new JLabel("<html><div style='text-align:center;'>" + result + "</div></html>", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        JButton back = new JButton("Back to Menu");
        JButton close = new JButton("Close");

        back.addActionListener(e -> {
            client.closeConnection();
            frame.closeFrame();
            new StartScreen();
            dispose();
        });
        close.addActionListener(e -> {
            dispose();
            frame.closeFrame();
            client.exitApplication();
        });

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(back);
        buttons.add(close);
        add(label, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setVisible(true);
    }
}
