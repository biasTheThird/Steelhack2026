package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.Msg;

/**
 * Who is playing and how they are doing. A player who has already guessed this
 * round is marked in teal; the person picking the target gets a caption instead
 * of a badge, because it is a role rather than an achievement.
 */
public final class LeaderboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel rows = new JPanel();
    private int selfId = -1;

    public LeaderboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.INK);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.EDGE),
                BorderFactory.createEmptyBorder(12, 14, 12, 10)));

        JLabel heading = new JLabel("Players");
        heading.setFont(Theme.TITLE);
        heading.setForeground(Theme.PAPER);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);

        JScrollPane scroller = new JScrollPane(rows);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(12);

        add(heading, BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);
        setPreferredSize(new Dimension(210, 300));
    }

    public void setSelfId(int id) {
        this.selfId = id;
    }

    public void refresh(List<Msg.Standing> standings) {
        rows.removeAll();

        if (standings == null || standings.isEmpty()) {
            rows.add(hint("Nobody here yet."));
        } else {
            for (Msg.Standing standing : standings) {
                rows.add(row(standing));
                rows.add(Box.createVerticalStrut(6));
            }
        }

        rows.add(Box.createVerticalGlue());
        rows.revalidate();
        rows.repaint();
    }

    private Component row(Msg.Standing standing) {
        JPanel row = new JPanel(new GridLayout(standing.drawer ? 2 : 1, 1));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel line = new JPanel(new BorderLayout(8, 0));
        line.setOpaque(false);

        String displayName = standing.id == selfId ? standing.name + " (you)" : standing.name;
        JLabel name = new JLabel(displayName);
        name.setFont(standing.id == selfId ? Theme.BODY_BOLD : Theme.BODY);
        name.setForeground(standing.guessed ? Theme.DEVELOPER : Theme.PAPER);

        JLabel points = new JLabel(String.valueOf(standing.points), SwingConstants.RIGHT);
        points.setFont(Theme.BODY);
        points.setForeground(Theme.GRAPHITE);

        line.add(name, BorderLayout.CENTER);
        line.add(points, BorderLayout.EAST);
        row.add(line);

        if (standing.drawer) {
            JLabel role = new JLabel("picking the target");
            role.setFont(Theme.SMALL);
            role.setForeground(Theme.GRAPHITE);
            row.add(role);
        }

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private Component hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.GRAPHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
