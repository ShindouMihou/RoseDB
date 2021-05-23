package pw.mihou.rosedb.connections;

import io.javalin.Javalin;
import io.javalin.core.compression.CompressionStrategy;
import io.javalin.websocket.WsContext;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.MessageTooLargeException;
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

    public static void reply(WsContext context, JSONObject response, String unique, int kode) {
        context.send(response.put("kode", kode).put("replyTo", unique).toString());
    }

    public static void reply(WsContext context, String response, String unique, int kode) {
        context.send(new JSONObject().put("response", response).put("kode", kode)
                .put("replyTo", unique).toString());
    }

    public static RoseDatabase getDatabase(String db) {
        if(!database.containsKey(db.toLowerCase())) {
            database.put(db.toLowerCase(), FileHandler.readDatabase(db.toLowerCase()).join());
        }

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
        RoseListenerManager.register(new AggregateListener());
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
        Terminal.log(Levels.DEBUG, "All listeners are registered.");

        if (Terminal.root.id == Levels.DEBUG.id) {
            Terminal.log(Levels.WARNING, "For maximum performance, we recommend turning off DEBUG mode unless needed (especially when requests can reach large sizes).");
        }

        if(RoseDB.preload){
            Terminal.log(Levels.INFO, "Pre-caching all data ahead of time, you may disable this behavior in config.json if you don't mind performance decrease.");
            FileHandler.preloadAll().thenAccept(unused -> Terminal.log(Levels.INFO, "All data are now pre-loaded into cache!"));
        } else {
            Terminal.log(Levels.WARNING, "Please note that disabling preloading will cause performance to be lower.");
        }

        Javalin app = Javalin.create(config -> {
            config.defaultContentType = "application/json";
            config.showJavalinBanner = false;
            config.compressionStrategy(CompressionStrategy.GZIP);
            config.wsFactoryConfig(ws -> {
                ws.getPolicy().setMaxTextMessageBufferSize(RoseDB.buffer * 1024 * 1024);
                ws.getPolicy().setMaxTextMessageSize(RoseDB.size * 1024 * 1024);
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
        Runtime.getRuntime().addShutdownHook(new Thread(FileHandler::executeFinalRuntime));

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
                } catch (MessageTooLargeException e){
                    reply(ctx, "The request was canceled by force: " + e.getMessage(), -1);
                    Terminal.log(Levels.ERROR, "Received message that was too large from" + ctx.session.getRemoteAddress().toString());
                }
            });
        });

        Terminal.log(Levels.INFO, "RoseDB is now running on port: " + port);
        startHeartbeat();
    }

}
