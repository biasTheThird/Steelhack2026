package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.Msg;

/**
 * Chat and guesses are the same field, the way they are in the game this borrows
 * from: you type what you think it is, and if you are right the server turns it
 * into an announcement instead of a message.
 */
public final class ChatPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextPane transcript = new JTextPane();
    private final JTextField entry = new JTextField();
    private final JLabel prompt = new JLabel("Type your guess");

    public ChatPanel(Consumer<String> onSend) {
        setLayout(new BorderLayout(0, 8));
        setBackground(Theme.INK);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.EDGE),
                BorderFactory.createEmptyBorder(12, 12, 12, 14)));
        setPreferredSize(new Dimension(280, 300));

        transcript.setEditable(false);
        transcript.setBackground(Theme.INK);
        transcript.setForeground(Theme.PAPER);
        transcript.setFont(Theme.BODY);
        transcript.setBorder(null);

        JScrollPane scroller = new JScrollPane(transcript);
        scroller.setBorder(null);
        scroller.getViewport().setBackground(Theme.INK);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(14);

        prompt.setFont(Theme.SMALL);
        prompt.setForeground(Theme.GRAPHITE);

        entry.setBackground(Theme.TRAY);
        entry.setForeground(Theme.PAPER);
        entry.setCaretColor(Theme.DEVELOPER);
        entry.setFont(Theme.BODY);
        entry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.EDGE),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)));
        entry.addActionListener(e -> {
            String text = entry.getText().trim();
            if (!text.isEmpty()) {
                entry.setText("");
                onSend.accept(text);
            }
        });

        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        bottom.add(prompt, BorderLayout.NORTH);
        bottom.add(entry, BorderLayout.CENTER);

        add(scroller, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void focusEntry() {
        entry.requestFocusInWindow();
    }

    public void setPrompt(String text) {
        prompt.setText(text);
    }

    public void add(Msg.Chat chat) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        String line;

        switch (chat.kind) {
            case SYSTEM:
                StyleConstants.setForeground(style, Theme.GRAPHITE);
                StyleConstants.setItalic(style, true);
                line = chat.text;
                break;
            case CORRECT:
                StyleConstants.setForeground(style, Theme.DEVELOPER);
                StyleConstants.setBold(style, true);
                line = chat.text;
                break;
            case CLOSE:
                StyleConstants.setForeground(style, Theme.FLARE);
                line = chat.text;
                break;
            default:
                StyleConstants.setForeground(style, Theme.PAPER);
                line = (chat.who == null ? "" : chat.who + ": ") + chat.text;
                break;
        }

        append(line, style);
    }

    private void append(String line, SimpleAttributeSet style) {
        StyledDocument document = transcript.getStyledDocument();
        try {
            document.insertString(document.getLength(), line + "\n", style);
        } catch (BadLocationException e) {
            // The document only grows at the end, so this should not happen.
            return;
        }
        transcript.setCaretPosition(document.getLength());
    }
}
