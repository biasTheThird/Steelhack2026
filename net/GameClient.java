package net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

/**
 * Talks to the server and delivers everything to the listener on the Swing
 * thread, so the UI never has to think about threads.
 */
public final class GameClient {

    public interface Listener {
        void onWelcome(Msg.Welcome welcome);

        void onPlayers(Msg.Players players);

        void onChat(Msg.Chat chat);

        void onChoices(Msg.Choices choices);

        void onLoading(Msg.Loading loading);

        void onBegin(Msg.Begin begin);

        void onTick(Msg.Tick tick);

        void onOver(Msg.Over over);

        void onStatus(Msg.Status status);

        void onDisconnect(String reason);
    }

    private static final Object POISON = new Object();

    private final BlockingQueue<Object> outbox = new LinkedBlockingQueue<>();

    private volatile Listener listener;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean open;

    /**
     * The window needs the client to send from, and the client needs the window to
     * deliver to, so the listener is set after both exist.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String playerName) throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);

        // Output first, matching the server, or both ends block on the stream header.
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        open = true;

        Thread writer = new Thread(this::writeLoop, "client-writer");
        writer.setDaemon(true);
        writer.start();

        Thread reader = new Thread(this::readLoop, "client-reader");
        reader.setDaemon(true);
        reader.start();

        send(new Msg.Join(playerName));
    }

    public void say(String text) {
        send(new Msg.Say(text));
    }

    public void pick(int index) {
        send(new Msg.Pick(index));
    }

    public void addPerson(String person, List<byte[]> images) {
        send(new Msg.AddPerson(person, images));
    }

    public void close() {
        if (!open) {
            return;
        }
        open = false;
        outbox.offer(POISON);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // Already gone.
        }
    }

    private void send(Object message) {
        if (open) {
            outbox.offer(message);
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                Object message = outbox.take();
                if (message == POISON) {
                    return;
                }
                out.reset();
                out.writeObject(message);
                out.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            fail("Lost the connection while sending.");
        }
    }

    private void readLoop() {
        try {
            while (open) {
                Object message = in.readObject();
                dispatch(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (open) {
                fail("Disconnected from the server.");
            }
        }
    }

    private void dispatch(final Object message) {
        SwingUtilities.invokeLater(() -> {
            if (listener == null) {
                return;
            }
            if (message instanceof Msg.Welcome) {
                listener.onWelcome((Msg.Welcome) message);
            } else if (message instanceof Msg.Players) {
                listener.onPlayers((Msg.Players) message);
            } else if (message instanceof Msg.Chat) {
                listener.onChat((Msg.Chat) message);
            } else if (message instanceof Msg.Choices) {
                listener.onChoices((Msg.Choices) message);
            } else if (message instanceof Msg.Loading) {
                listener.onLoading((Msg.Loading) message);
            } else if (message instanceof Msg.Begin) {
                listener.onBegin((Msg.Begin) message);
            } else if (message instanceof Msg.Tick) {
                listener.onTick((Msg.Tick) message);
            } else if (message instanceof Msg.Over) {
                listener.onOver((Msg.Over) message);
            } else if (message instanceof Msg.Status) {
                listener.onStatus((Msg.Status) message);
            }
        });
    }

    private void fail(final String reason) {
        if (!open) {
            return;
        }
        open = false;
        SwingUtilities.invokeLater(() -> {
            if (listener != null) {
                listener.onDisconnect(reason);
            }
        });
    }
}
