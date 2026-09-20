package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import game.Config;

/**
 * Adds someone to the answer pool. Type a name, attach photos, send.
 *
 * The photos go to the server, not into this client's own library, so everyone
 * gets them. The server books the new person a slot inside the next seven rounds
 * and says which one in chat.
 */
public final class UploadPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public interface Submit {
        void add(String person, List<byte[]> images);
    }

    private final JTextField nameField = new JTextField();
    private final JLabel attached = new JLabel("No photos attached");
    private final JButton chooseButton = new JButton("Choose photos");
    private final JButton sendButton = new JButton("Add to the game");
    private final Submit submit;

    private final List<File> selected = new ArrayList<>();

    public UploadPanel(Submit submit) {
        this.submit = submit;

        setLayout(new BorderLayout(0, 8));
        setBackground(Theme.INK);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 1, Theme.EDGE),
                BorderFactory.createEmptyBorder(12, 14, 14, 10)));
        setPreferredSize(new Dimension(210, 190));

        JLabel heading = new JLabel("Add someone to guess");
        heading.setFont(Theme.TITLE);
        heading.setForeground(Theme.PAPER);

        JLabel help = new JLabel("They'll be the target within "
                + Config.GUARANTEE_ROUNDS + " rounds.");
        help.setFont(Theme.SMALL);
        help.setForeground(Theme.GRAPHITE);

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(help, BorderLayout.SOUTH);

        nameField.setBackground(Theme.TRAY);
        nameField.setForeground(Theme.PAPER);
        nameField.setCaretColor(Theme.DEVELOPER);
        nameField.setFont(Theme.BODY);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.EDGE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        attached.setFont(Theme.SMALL);
        attached.setForeground(Theme.GRAPHITE);

        style(chooseButton);
        style(sendButton);
        sendButton.setForeground(Theme.DEVELOPER);

        chooseButton.addActionListener(e -> choose());
        sendButton.addActionListener(e -> send());

        JPanel middle = new JPanel(new GridLayout(3, 1, 0, 6));
        middle.setOpaque(false);
        middle.add(nameField);
        middle.add(chooseButton);
        middle.add(attached);

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(sendButton, BorderLayout.SOUTH);
    }

    private void style(JButton button) {
        button.setFont(Theme.BODY);
        button.setForeground(Theme.PAPER);
        button.setBackground(Theme.TRAY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.EDGE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void choose() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Pick photos of one person");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Images", "jpg", "jpeg", "png", "gif", "bmp"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        selected.clear();
        File[] files = chooser.getSelectedFiles();
        if (files != null) {
            for (File file : files) {
                if (selected.size() >= Config.MAX_UPLOAD_IMAGES) {
                    break;
                }
                selected.add(file);
            }
        }

        attached.setText(selected.isEmpty()
                ? "No photos attached"
                : selected.size() + (selected.size() == 1 ? " photo attached" : " photos attached"));
        attached.setForeground(selected.isEmpty() ? Theme.GRAPHITE : Theme.DEVELOPER);
    }

    private void send() {
        String person = nameField.getText().trim();
        if (person.isEmpty()) {
            attached.setText("Give them a name first");
            attached.setForeground(Theme.FLARE);
            nameField.requestFocusInWindow();
            return;
        }
        if (selected.isEmpty()) {
            attached.setText("Attach at least one photo");
            attached.setForeground(Theme.FLARE);
            return;
        }

        List<byte[]> payload = new ArrayList<>();
        int skipped = 0;
        for (File file : selected) {
            try {
                if (file.length() > Config.MAX_UPLOAD_BYTES) {
                    skipped++;
                    continue;
                }
                payload.add(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                skipped++;
            }
        }

        if (payload.isEmpty()) {
            attached.setText("Couldn't read those files");
            attached.setForeground(Theme.FLARE);
            return;
        }

        submit.add(person, payload);

        selected.clear();
        nameField.setText("");
        attached.setText(skipped > 0 ? "Sent, " + skipped + " too large" : "Sent");
        attached.setForeground(Theme.GRAPHITE);
    }
}
