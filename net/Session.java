package net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * One connected player, from the server's side.
 *
 * Outbound messages go through a queue and a dedicated writer thread. A morph
 * payload is a few hundred kilobytes, so writing straight from the game loop
 * would let one player on bad wifi hold up everybody else's round.
 */
public final class Session {

    public interface Handler {
        void onJoin(Session session, String name);

        void onSay(Session session, String text);

        void onPick(Session session, int index);

        void onAddPerson(Session session, String person, List<byte[]> images);

        void onLeave(Session session);
    }

    private static final Object POISON = new Object();

    private final Socket socket;
    private final Handler handler;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final BlockingQueue<Object> outbox = new LinkedBlockingQueue<>();

    private volatile boolean open = true;

    public volatile int playerId = -1;
    public volatile String name = "";

    public Session(Socket socket, Handler handler) throws IOException {
        this.socket = socket;
        this.handler = handler;
        socket.setTcpNoDelay(true);

        // Output first on both ends. ObjectInputStream blocks reading the stream
        // header, so two peers that both open their input first will deadlock.
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void start() {
        Thread writer = new Thread(this::writeLoop, "session-writer");
        writer.setDaemon(true);
        writer.start();

        Thread reader = new Thread(this::readLoop, "session-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public void send(Object message) {
        if (open) {
            outbox.offer(message);
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        if (!open) {
            return;
        }
        open = false;
        outbox.offer(POISON);
        try {
            socket.close();
        } catch (IOException ignored) {
            // Already gone.
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                Object message = outbox.take();
                if (message == POISON) {
                    return;
                }
                // reset() stops the stream from remembering every object it has ever
                // written, which otherwise leaks a whole round of pixel data per round.
                out.reset();
                out.writeObject(message);
                out.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            close();
        }
    }

    @SuppressWarnings("unchecked")
    private void readLoop() {
        try {
            while (open) {
                Object message = in.readObject();

                if (message instanceof Msg.Join) {
                    handler.onJoin(this, ((Msg.Join) message).name);
                } else if (message instanceof Msg.Say) {
                    handler.onSay(this, ((Msg.Say) message).text);
                } else if (message instanceof Msg.Pick) {
                    handler.onPick(this, ((Msg.Pick) message).index);
                } else if (message instanceof Msg.AddPerson) {
                    Msg.AddPerson add = (Msg.AddPerson) message;
                    handler.onAddPerson(this, add.person, add.images);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Normal on disconnect.
        } finally {
            close();
            handler.onLeave(this);
        }
    }
}
