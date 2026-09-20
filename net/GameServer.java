package net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import game.Config;
import game.ImageLibrary;
import game.RoundEngine;
import game.Transport;

/**
 * Accepts connections and hands them to the round loop. Deliberately thin: it
 * owns sockets and nothing else about the game.
 */
public final class GameServer implements Session.Handler, Transport {

    private final int port;
    private final ImageLibrary library;
    private final RoundEngine engine;
    private final List<Session> sessions = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private ServerSocket serverSocket;
    private volatile boolean running;

    public GameServer(int port) {
        this.port = port;
        this.library = new ImageLibrary();
        this.engine = new RoundEngine(this, library);
    }

    public ImageLibrary library() {
        return library;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        Thread accepts = new Thread(this::acceptLoop, "server-accept");
        accepts.setDaemon(true);
        accepts.start();

        Thread rounds = new Thread(engine, "round-engine");
        rounds.setDaemon(true);
        rounds.start();

        String problem = library.readinessProblem();
        System.out.println("Morph guesser listening on port " + port);
        System.out.println(problem == null
                ? "Image library loaded from " + Config.SOURCE_DIR + " and " + Config.TARGET_DIR + "."
                : "Heads up: " + problem);
    }

    public void stop() {
        running = false;
        engine.stop();
        for (Session session : sessions) {
            session.close();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Shutting down anyway.
            }
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Session session = new Session(socket, this);
                sessions.add(session);
                session.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Dropped an incoming connection: " + e.getMessage());
                }
            }
        }
    }

    /* ----- Transport ----- */

    @Override
    public void toAll(Object message) {
        for (Session session : sessions) {
            if (session.playerId > 0) {
                session.send(message);
            }
        }
    }

    @Override
    public void toOne(int playerId, Object message) {
        for (Session session : sessions) {
            if (session.playerId == playerId) {
                session.send(message);
                return;
            }
        }
    }

    /* ----- Session.Handler ----- */

    @Override
    public void onJoin(Session session, String rawName) {
        if (session.playerId > 0) {
            return;
        }

        String name = cleanName(rawName);
        session.playerId = nextId.getAndIncrement();
        session.name = name;

        session.send(new Msg.Welcome(session.playerId, name));
        engine.addPlayer(session.playerId, name);
        session.send(engine.currentStatus());
        toAll(new Msg.Chat(null, name + " joined.", Msg.ChatKind.SYSTEM));
    }

    @Override
    public void onSay(Session session, String text) {
        if (session.playerId > 0) {
            engine.onSay(session.playerId, text);
        }
    }

    @Override
    public void onPick(Session session, int index) {
        if (session.playerId > 0) {
            engine.onPick(session.playerId, index);
        }
    }

    @Override
    public void onAddPerson(Session session, String person, List<byte[]> images) {
        if (session.playerId > 0) {
            engine.onAddPerson(session.playerId, person, images);
        }
    }

    @Override
    public void onLeave(Session session) {
        sessions.remove(session);
        if (session.playerId > 0) {
            engine.removePlayer(session.playerId);
        }
    }

    private String cleanName(String raw) {
        String name = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9 _'\\-]", "").trim();
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        if (name.isEmpty()) {
            name = "Player " + nextId.get();
        }
        return name;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Config.PORT;
        GameServer server = new GameServer(port);
        server.start();

        // Headless mode: run until killed.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.stop();
        }
    }
}
