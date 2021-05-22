package pw.mihou.rosedb.connections;

import io.javalin.Javalin;
import io.javalin.core.compression.CompressionStrategy;
import io.javalin.websocket.WsContext;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.listeners.*;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.RoseListenerManager;
import pw.mihou.rosedb.utility.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RoseServer {

    private static final Map<String, WsContext> context = new ConcurrentHashMap<>();
    private static final Map<String, RoseDatabase> database = new ConcurrentHashMap<>();

    public static void reply(WsContext context, String response, int kode) {
        context.send(new JSONObject().put("response", response).put("kode", kode).toString());
    }

    public static void reply(WsContext context, String response, String unique, int kode) {
        context.send(new JSONObject().put("response", response).put("kode", kode)
                .put("replyTo", unique).toString());
    }

    public static RoseDatabase getDatabase(String db) {
        database.putIfAbsent(db.toLowerCase(), FileHandler.readDatabase(db.toLowerCase()));
        return database.get(db.toLowerCase());
    }

    public static void removeDatabase(String db) throws IOException {
        database.remove(db.toLowerCase());
        FileUtils.deleteDirectory(new File(String.format(RoseDB.directory + "/%s/", db)));
    }

    private static void register() {
        RoseListenerManager.register(new RequestListener());
        RoseListenerManager.register(new AddListener());
        RoseListenerManager.register(new DeleteListener());
        RoseListenerManager.register(new UpdateListener());
        RoseListenerManager.register(new DropListener());
    }

    private static void startHeartbeat() {
        Scheduler.schedule(() -> context.values()
                .stream()
                .filter(wsContext -> wsContext.session.isOpen())
                .forEach(wsContext -> wsContext.send(new JSONObject()
                        .put("session", wsContext.getSessionId())
                        .put("kode", 0).toString())), 30, 30, TimeUnit.SECONDS);
        Terminal.log(Levels.DEBUG, "Heartbeat listener is now active.");
    }

    public static void run(int port) {
        register();

        System.out.println(" ______  ______  ______  ______  _____   ______    \n" +
                "/\\  == \\/\\  __ \\/\\  ___\\/\\  ___\\/\\  __-./\\  == \\   \n" +
                "\\ \\  __<\\ \\ \\/\\ \\ \\___  \\ \\  __\\\\ \\ \\/\\ \\ \\  __<   \n" +
                " \\ \\_\\ \\_\\ \\_____\\/\\_____\\ \\_____\\ \\____-\\ \\_____\\ \n" +
                "  \\/_/ /_/\\/_____/\\/_____/\\/_____/\\/____/ \\/_____/");
        Terminal.log(Levels.DEBUG, "All listeners are registered.");

        if (Terminal.root.id == Levels.DEBUG.id) {
            Terminal.log(Levels.WARNING, "For maximum performance, we recommend turning off DEBUG mode unless needed (especially when requests can reach large sizes).");
        }

        Javalin app = Javalin.create(config -> {
            config.compressionStrategy(CompressionStrategy.GZIP);
            config.wsFactoryConfig(ws -> {
                ws.getPolicy().setMaxTextMessageBufferSize(1024 * 5000);
                ws.getPolicy().setMaxTextMessageSize(1024 * 5000);
            });

            if (Levels.DEBUG.id <= Terminal.root.id) {
                config.wsLogger(wsHandler -> {
                    wsHandler.onConnect(wsConnectContext -> Terminal.log(Levels.DEBUG, "Received connection from " + wsConnectContext.session.getRemoteAddress().toString()));
                    wsHandler.onClose(wsCloseContext -> Terminal.log(Levels.DEBUG, "Received closed connection from " + wsCloseContext.session.getRemoteAddress().toString() + " for " + wsCloseContext.reason()));
                    wsHandler.onMessage(ctx -> Terminal.log(Levels.DEBUG, "Received request: " + ctx.message() + " from " + ctx.session.getRemoteAddress().toString()));
                });
            }
        });

        app.events(event -> {
            if (Levels.DEBUG.id <= Terminal.root.id) {
                event.serverStarting(() -> Terminal.log(Levels.DEBUG, "The server is now starting at port: " + port));
                event.serverStarted(() -> Terminal.log(Levels.DEBUG, "The server has started on port: " + port));
            }

            event.serverStartFailed(() -> Terminal.log(Levels.ERROR, "The server failed to start, possibly another instance at the same port is running."));
            event.serverStopping(() -> Terminal.log(Levels.INFO, "The server is now shutting off."));
            event.serverStopped(() -> Terminal.log(Levels.INFO, "The server has successfully closed."));
        }).start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        Terminal.log(Levels.DEBUG, "All events and handlers are now ready.");

        app.ws("/", ws -> {
            ws.onConnect(ctx -> context.put(ctx.getSessionId(), ctx));
            ws.onClose(ctx -> context.remove(ctx.getSessionId()));
            ws.onMessage(ctx -> {
                try {
                    RoseListenerManager.execute(new JSONObject(ctx.message()), ctx);
                } catch (JSONException e) {
                    reply(ctx, "The request was considered as invalid: " + e.getMessage(), -1);
                    Terminal.log(Levels.DEBUG, "Received invalid JSON request: " + ctx.message() + " from " + ctx.session.getRemoteAddress().toString());
                }
            });
        });

        Terminal.log(Levels.INFO, "RoseDB is now running on port: " + port);
        startHeartbeat();
    }

}
