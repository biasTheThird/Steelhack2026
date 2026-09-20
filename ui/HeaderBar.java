package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import game.Config;

/**
 * The blanks are the loudest thing in the window on purpose - they are what
 * everybody is actually looking at. Round number and clock stay quiet at the
 * edges.
 */
public final class HeaderBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel roundLabel = new JLabel("Round 0");
    private final JLabel questionLabel = new JLabel("Waiting to start", SwingConstants.CENTER);
    private final JLabel blanksLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel clockLabel = new JLabel("--", SwingConstants.RIGHT);
    private final JLabel clockCaption = new JLabel("seconds left", SwingConstants.RIGHT);

    public HeaderBar() {
        setLayout(new BorderLayout());
        setBackground(Theme.INK);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.EDGE),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        roundLabel.setFont(Theme.TITLE);
        roundLabel.setForeground(Theme.PAPER);
        roundLabel.setVerticalAlignment(SwingConstants.TOP);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(150, 56));
        left.add(roundLabel);

        questionLabel.setFont(Theme.SMALL);
        questionLabel.setForeground(Theme.GRAPHITE);
        blanksLabel.setFont(Theme.BLANKS);
        blanksLabel.setForeground(Theme.PAPER);

        JPanel centre = new JPanel(new BorderLayout(0, 2));
        centre.setOpaque(false);
        centre.add(questionLabel, BorderLayout.NORTH);
        centre.add(blanksLabel, BorderLayout.CENTER);

        clockLabel.setFont(Theme.CLOCK);
        clockLabel.setForeground(Theme.PAPER);
        clockCaption.setFont(Theme.SMALL);
        clockCaption.setForeground(Theme.GRAPHITE);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(150, 56));
        right.add(clockLabel, BorderLayout.CENTER);
        right.add(clockCaption, BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);
        add(centre, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public void setRound(int round) {
        roundLabel.setText(round <= 0 ? "No round yet" : "Round " + round);
    }

    /** The question line above the blanks. */
    public void setQuestion(String text) {
        questionLabel.setText(text == null ? " " : text);
    }

    public void setBlanks(String mask, int letters) {
        if (mask == null || mask.isEmpty()) {
            blanksLabel.setText(" ");
            return;
        }
        blanksLabel.setText(mask);
        questionLabel.setText("What is this morphing into?   " + letters
                + (letters == 1 ? " letter" : " letters"));
    }

    public void showAnswer(String answer) {
        blanksLabel.setText(answer);
        blanksLabel.setForeground(Theme.DEVELOPER);
        questionLabel.setText("The answer");
    }

    public void clearAnswer() {
        blanksLabel.setForeground(Theme.PAPER);
    }

    public void setClock(int secondsLeft) {
        if (secondsLeft < 0) {
            clockLabel.setText("--");
            clockLabel.setForeground(Theme.PAPER);
            return;
        }
        clockLabel.setText(String.valueOf(secondsLeft));
        boolean urgent = secondsLeft <= 10 || secondsLeft <= Config.ROUND_SECONDS / 8;
        clockLabel.setForeground(urgent ? Theme.FLARE : Theme.PAPER);
    }

    public void setClockCaption(String caption) {
        clockCaption.setText(caption);
    }
}
