package pw.mihou.rosedb.connections;

import ch.qos.logback.classic.Level;
import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.io.writers.RoseWriter;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.RoseListenerManager;
import pw.mihou.rosedb.server.ServerApplication;
import pw.mihou.rosedb.utility.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RoseServer {

    public static final Map<String, WebSocket> context = new ConcurrentHashMap<>();
    private static final Scanner scanner = new Scanner(System.in).useDelimiter("\n");
    public static boolean isOpen = true;

    /**
     * Replies to the client that doesn't have
     * a unique field.
     *
     * @param context The websocket context.
     * @param response The response to send.
     * @param kode The kode response.
     */
    public static void reply(WebSocket context, String response, int kode) {
        reply(context, new JSONObject().put("response", response).put("kode", kode));
    }

    /**
     * Replies to the client in full detail,
     * this includes a unique field.
     *
     * @param context The websocket context.
     * @param response The response to send.
     * @param unique The unique callback field received.
     * @param kode The kode response.
     */
    public static void reply(WebSocket context, JSONObject response, String unique, int kode) {
        reply(context, response.put("kode", kode).put("replyTo", unique));
    }

    /**
     * Replies to the client in full detail but
     * with a String argument.
     *
     * @param context The websocket context.
     * @param response The response to send.
     * @param unique The unique callback field received.
     * @param kode The kode response.
     */
    public static void reply(WebSocket context, String response, String unique, int kode) {
        reply(context, new JSONObject().put("response", response).put("kode", kode)
                .put("replyTo", unique));
    }

    /**
     * Replies to the client.
     *
     * @param context The websocket context.
     * @param response The response including any other add-ons.
     */
    public static void reply(WebSocket context, JSONObject response){
        if(context == null)
            Terminal.log(Levels.INFO, response.toString());
        else
            context.send(response.toString());
    }

    /**
     * Starts the heartbeat sending towards
     * all the client to keep them from disconnecting
     * because of timeout.
     */
    private static void startHeartbeat() {
        Scheduler.schedule(() -> context.values()
                .stream()
                .filter(WebSocket::isOpen)
                .forEach(wsContext -> {
                    wsContext.send(new JSONObject()
                            .put("session", (String) wsContext.getAttachment())
                            .toString());
                    wsContext.sendPing();
                }), RoseDB.heartbeat, RoseDB.heartbeat, TimeUnit.SECONDS);
        Terminal.log(Levels.DEBUG, "Heartbeat listener is now active.");
    }

    /**
     * Starts the writing service of the application.
     * This will write all requests that have gathered around
     * within five seconds.
     */
    private static void startWriter() {
        Scheduler.schedule(RoseWriter::write, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Starts and runs the websocket server
     * of the application. This should only be called once
     * and it should be done at main().
     *
     * @param port The port which the server will be running at.
     */
    public static void run(int port) {
        if (Terminal.root == Level.DEBUG) {
            Terminal.log(Levels.WARNING, "For maximum performance, we recommend turning off DEBUG mode unless needed (especially when requests can reach large sizes).");
        }

        if (RoseDB.preload) {
            Terminal.log(Levels.INFO, "Pre-caching all data ahead of time, you may disable this behavior in config.json if you don't mind performance decrease.");
            FileHandler.preloadAll();
            Terminal.log(Levels.INFO, "All data are now pre-loaded into cache!");
        } else {
            Terminal.log(Levels.WARNING, "Please note that disabling preloading will cause performance to be lower.");
        }

        ServerApplication app = new ServerApplication(port);
        app.start();

        if (RoseDB.versioning) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> RoseDatabase.databases.asMap().values().forEach(roseDatabase -> roseDatabase.getCollections()
                    .forEach(collections -> collections.getVersions().forEach((s, s2) -> {
                        String location = ".rose_versions" + File.separator + roseDatabase.getDatabase() + File.separator +
                                collections.collection + File.separator;

                        if (!new File(location).exists()) {
                            boolean mkdirs = new File(location).mkdirs();
                            if (!mkdirs) {
                                Terminal.setLoggingLevel(Level.ERROR);
                                Terminal.log(Levels.ERROR, "Failed to create folders for " + location + ", possibly we do not have permission to write.");
                            }
                        }

                        RoseWriter.writeGZIP(location + s + ".rose", s2);
                    })))));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> isOpen = false));
        Runtime.getRuntime().addShutdownHook(new Thread(RoseWriter::write));
        Runtime.getRuntime().addShutdownHook(new Thread(Scheduler::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                app.stop();
            } catch (IOException | InterruptedException exception) {
                Terminal.log(Levels.ERROR, "An exception occurred while trying to close server: {}", exception.getMessage());
            }
        }));

        Terminal.log(Levels.DEBUG, "All events and handlers are now ready.");

        Terminal.log(Levels.INFO, "RoseDB is now running on port: {}", port);
        startHeartbeat();
        startWriter();

        while (scanner.hasNextLine()) {
            String request = scanner.nextLine();
            QueryRequest query = RoseQuery.parse(request);
            try {
                if(query.isValid()){
                    RoseListenerManager.execute(query, null);
                } else {
                    RoseListenerManager.execute(new JSONObject(request), null);
                }
            } catch (JSONException e) {
                reply(null, "The request was considered as invalid: " + request, -1);
                Terminal.log(Levels.DEBUG, "Received invalid JSON request: {} from terminal", request);
            }
        }
    }

}
