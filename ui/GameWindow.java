package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import game.Config;
import net.GameClient;
import net.Msg;

/**
 * One window, four regions.
 *
 *   +--------------------------------------------------------------+
 *   | Round 4      What is this morphing into?           37        |
 *   |              _ _ _ _   _ _ _ _ _                 seconds     |
 *   +-------------+--------------------------------+---------------+
 *   | Players     |                                | chat          |
 *   |             |   the morph, or the picker,    |               |
 *   |             |   or the progress bar          |               |
 *   +-------------+                                |               |
 *   | Add someone |                                | [ guess     ] |
 *   +-------------+--------------------------------+---------------+
 *
 * The centre is a card stack because those three things never appear at once.
 */
public final class GameWindow extends JFrame implements GameClient.Listener {

    private static final long serialVersionUID = 1L;

    private static final String CARD_CANVAS = "canvas";
    private static final String CARD_CHOICES = "choices";
    private static final String CARD_MESSAGE = "message";

    private final GameClient client;

    private final HeaderBar header = new HeaderBar();
    private final LeaderboardPanel leaderboard = new LeaderboardPanel();
    private final UploadPanel upload;
    private final ChatPanel chat;
    private final MorphCanvasPanel canvas = new MorphCanvasPanel();
    private final ChoicePanel choices;
    private final MessagePanel message = new MessagePanel();

    private final CardLayout cards = new CardLayout();
    private final JPanel stage = new JPanel(cards);

    private int selfId = -1;
    private boolean drawingThisRound;
    private int letterCount;

    public GameWindow(GameClient client) {
        super("Morph guesser");
        this.client = client;

        chat = new ChatPanel(this::onSend);
        upload = new UploadPanel(client::addPerson);
        choices = new ChoicePanel(client::pick);

        stage.setBackground(Theme.TRAY);
        stage.add(canvas, CARD_CANVAS);
        stage.add(choices, CARD_CHOICES);
        stage.add(message, CARD_MESSAGE);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Theme.INK);
        left.setPreferredSize(new Dimension(212, 400));
        left.add(leaderboard, BorderLayout.CENTER);
        left.add(upload, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.TRAY);
        root.add(header, BorderLayout.NORTH);
        root.add(left, BorderLayout.WEST);
        root.add(stage, BorderLayout.CENTER);
        root.add(chat, BorderLayout.EAST);

        setContentPane(root);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 600));
        setSize(1000, 660);
        setLocationRelativeTo(null);

        message.message("Connecting", "");
        cards.show(stage, CARD_MESSAGE);
        leaderboard.refresh(null);
    }

    private void onSend(String text) {
        client.say(text);
    }

    /* ----- GameClient.Listener ----- */

    @Override
    public void onWelcome(Msg.Welcome welcome) {
        selfId = welcome.playerId;
        leaderboard.setSelfId(selfId);
        setTitle("Morph guesser  -  " + welcome.name);
        chat.focusEntry();
    }

    @Override
    public void onPlayers(Msg.Players players) {
        List<Msg.Standing> standings = players.standings;
        leaderboard.refresh(standings);
    }

    @Override
    public void onChat(Msg.Chat line) {
        chat.add(line);
    }

    @Override
    public void onChoices(Msg.Choices incoming) {
        drawingThisRound = true;
        choices.present(incoming.names, incoming.previews);
        cards.show(stage, CARD_CHOICES);

        header.setQuestion("Your turn to pick");
        header.setBlanks("", 0);
        header.setClockCaption("to choose");
        chat.setPrompt("You're picking, so no guessing this round");
    }

    @Override
    public void onLoading(Msg.Loading loading) {
        message.progress(loading.percent, loading.note);
        cards.show(stage, CARD_MESSAGE);
        header.setClock(-1);
        header.setClockCaption("building");
    }

    @Override
    public void onBegin(Msg.Begin begin) {
        drawingThisRound = begin.answer != null;

        header.setRound(begin.round);
        header.clearAnswer();
        letterCount = begin.letters;
        header.setBlanks(begin.mask, begin.letters);
        header.setClock(begin.seconds);
        header.setClockCaption("seconds left");

        if (drawingThisRound) {
            header.setQuestion("You picked " + begin.answer + ". Let them sweat.");
            chat.setPrompt("You already know it");
        } else {
            chat.setPrompt("Type your guess");
            chat.focusEntry();
        }

        canvas.play(begin.morph);
        cards.show(stage, CARD_CANVAS);
    }

    @Override
    public void onTick(Msg.Tick tick) {
        header.setClock(tick.secondsLeft);
        if (tick.mask != null && !tick.mask.isEmpty() && !drawingThisRound) {
            header.setBlanks(tick.mask, letterCount);
        }
    }

    @Override
    public void onOver(Msg.Over over) {
        canvas.settle();
        header.showAnswer(over.answer);
        header.setClock(-1);
        header.setClockCaption("round over");
        chat.add(new Msg.Chat(null, "It was " + over.answer + ". " + over.note, Msg.ChatKind.SYSTEM));
        drawingThisRound = false;
    }

    @Override
    public void onStatus(Msg.Status status) {
        header.setRound(status.round);

        switch (status.phase) {
            case LOBBY:
                canvas.clear();
                message.message(status.note, "Anyone on your network can join on port " + Config.PORT + ".");
                cards.show(stage, CARD_MESSAGE);
                header.setClock(-1);
                header.setQuestion("Waiting to start");
                header.setBlanks("", 0);
                break;
            case CHOOSING:
                if (!drawingThisRound) {
                    message.message(status.note, "");
                    cards.show(stage, CARD_MESSAGE);
                    header.setQuestion("Waiting for the target");
                    header.setBlanks("", 0);
                    header.setClockCaption("to choose");
                }
                break;
            case LOADING:
                message.progress(0, status.note);
                cards.show(stage, CARD_MESSAGE);
                break;
            case PLAYING:
                message.message(status.note, "");
                cards.show(stage, CARD_MESSAGE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onDisconnect(String reason) {
        canvas.clear();
        message.message(reason, "Restart the client to try again.");
        cards.show(stage, CARD_MESSAGE);
        header.setClock(-1);
        chat.setPrompt("Not connected");
    }
}
