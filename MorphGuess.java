import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import game.Config;
import net.GameClient;
import net.GameServer;
import ui.GameWindow;
import ui.Theme;

/**
 * Entry point.
 *
 *   java MorphGuess              opens the setup dialog
 *   java MorphGuess --server     runs a headless server and nothing else
 *
 * Hosting starts a server inside this process and then joins it over the loopback
 * address, so the host is an ordinary player with no special path through the code.
 */
public final class MorphGuess {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--server")) {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : Config.PORT;
            try {
                new GameServer(port).start();
                Thread.currentThread().join();
            } catch (IOException e) {
                System.err.println("Couldn't start the server: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        SwingUtilities.invokeLater(MorphGuess::showSetup);
    }

    private static void showSetup() {
        final JTextField nameField = new JTextField(System.getProperty("user.name", ""), 14);
        final JTextField hostField = new JTextField("localhost", 14);
        final JTextField portField = new JTextField(String.valueOf(Config.PORT), 14);

        final JRadioButton hostChoice = new JRadioButton("Host a game on this machine", true);
        final JRadioButton joinChoice = new JRadioButton("Join a game");
        ButtonGroup group = new ButtonGroup();
        group.add(hostChoice);
        group.add(joinChoice);

        hostField.setEnabled(false);
        hostChoice.addActionListener(e -> hostField.setEnabled(false));
        joinChoice.addActionListener(e -> hostField.setEnabled(true));

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.setBackground(Theme.INK);
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Morph guesser");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.PAPER);
        form.add(title);

        JLabel blurb = new JLabel("A picture dissolves into a photo of someone. First to name them wins.");
        blurb.setFont(Theme.SMALL);
        blurb.setForeground(Theme.GRAPHITE);
        form.add(blurb);

        form.add(label("Your name"));
        form.add(nameField);
        form.add(hostChoice);
        form.add(joinChoice);
        form.add(label("Server address"));
        form.add(hostField);
        form.add(label("Port"));
        form.add(portField);

        JButton go = new JButton("Start");
        go.setFont(Theme.BODY);
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setBackground(Theme.INK);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));
        buttons.add(go, BorderLayout.EAST);

        final JDialog dialog = new JDialog((java.awt.Frame) null, "Morph guesser", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(Theme.INK);
        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.setMinimumSize(new Dimension(340, 400));
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        go.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.requestFocusInWindow();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException bad) {
                JOptionPane.showMessageDialog(dialog, "That port isn't a number.");
                return;
            }

            String host = hostChoice.isSelected() ? "localhost" : hostField.getText().trim();

            if (hostChoice.isSelected()) {
                try {
                    new GameServer(port).start();
                } catch (IOException failure) {
                    JOptionPane.showMessageDialog(dialog,
                            "Couldn't listen on port " + port + ". Something else may already be using it.");
                    return;
                }
            }

            dialog.dispose();
            openGame(host, port, name);
        });

        dialog.setVisible(true);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.GRAPHITE);
        return label;
    }

    private static void openGame(String host, int port, String name) {
        GameClient client = new GameClient();
        GameWindow window = new GameWindow(client);
        client.setListener(window);
        window.setVisible(true);

        try {
            client.connect(host, port, name);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(window,
                    "Couldn't reach " + host + ":" + port + ". Check the address and that the host has started a game.");
        }
    }
}
