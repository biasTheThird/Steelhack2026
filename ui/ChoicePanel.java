package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The drawer's one decision. Three photos, click the one you want everybody to
 * chase. The name under each is the answer they will be typing, so it is shown
 * plainly rather than teased.
 */
public final class ChoicePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public interface Pick {
        void picked(int index);
    }

    private final Pick pick;
    private final JPanel options = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
    private final JLabel heading = new JLabel("Pick what the picture turns into", SwingConstants.CENTER);
    private final List<JButton> buttons = new ArrayList<>();

    public ChoicePanel(Pick pick) {
        this.pick = pick;

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.TRAY);
        setBorder(BorderFactory.createEmptyBorder(28, 20, 28, 20));

        heading.setFont(Theme.TITLE);
        heading.setForeground(Theme.PAPER);
        options.setOpaque(false);

        add(heading, BorderLayout.NORTH);
        add(options, BorderLayout.CENTER);
    }

    public void present(List<String> names, List<byte[]> previews) {
        options.removeAll();
        buttons.clear();
        heading.setText("Pick what the picture turns into");

        for (int i = 0; i < names.size(); i++) {
            options.add(option(i, names.get(i), i < previews.size() ? previews.get(i) : null));
        }

        options.revalidate();
        options.repaint();
    }

    private JPanel option(final int index, String name, byte[] preview) {
        JPanel cell = new JPanel(new BorderLayout(0, 8));
        cell.setOpaque(false);

        JButton button = new JButton();
        button.setPreferredSize(new Dimension(150, 150));
        button.setBackground(Theme.INK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Theme.EDGE, 1));
        button.setToolTipText(name);

        if (preview != null) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(preview));
                if (image != null) {
                    button.setIcon(new ImageIcon(image));
                }
            } catch (IOException e) {
                button.setText("No preview");
                button.setForeground(Theme.GRAPHITE);
            }
        } else {
            button.setText("No preview");
            button.setForeground(Theme.GRAPHITE);
        }

        buttons.add(button);
        button.addActionListener(e -> {
            heading.setText("Locked in. Building the morph.");
            for (JButton other : buttons) {
                other.setEnabled(false);
            }
            pick.picked(index);
        });

        JLabel label = new JLabel(name, SwingConstants.CENTER);
        label.setFont(Theme.BODY);
        label.setForeground(Theme.PAPER);

        cell.add(button, BorderLayout.CENTER);
        cell.add(label, BorderLayout.SOUTH);
        return cell;
    }
}
