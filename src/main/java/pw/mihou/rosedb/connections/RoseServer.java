package pw.mihou.rosedb.connections;

import ch.qos.logback.classic.Level;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.RoseListenerManager;
import pw.mihou.rosedb.utility.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RoseServer {

    private static final Map<String, WsContext> context = new ConcurrentHashMap<>();
    private static final LoadingCache<String, RoseDatabase> database = Caffeine.newBuilder()
            .build(key -> FileHandler.readDatabase(key.toLowerCase()).join());
    private static final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    public static void reply(WsContext context, String response, int kode) {
        if(context != null) {
            context.send(new JSONObject().put("response", response).put("kode", kode).toString());
        } else {
            Terminal.log(Levels.INFO, new JSONObject().put("response", response).put("kode", kode).toString());
        }
    }

    public static void reply(WsContext context, JSONObject response, String unique, int kode) {
        if(context != null) {
            context.send(response.put("kode", kode).put("replyTo", unique).toString());
        } else {
            Terminal.log(Levels.INFO, response.put("kode", kode).put("replyTo", unique).toString());
        }
    }

    public static void reply(WsContext context, String response, String unique, int kode) {
        if(context != null) {
            context.send(new JSONObject().put("response", response).put("kode", kode)
                    .put("replyTo", unique).toString());
        } else {
            Terminal.log(Levels.INFO, new JSONObject().put("response", response).put("kode", kode)
                    .put("replyTo", unique).toString());
        }
    }

    public static RoseDatabase getDatabase(String db) {
        return database.get(db.toLowerCase());
    }

    public static synchronized void removeDatabase(String db) throws IOException {
        database.invalidate(db.toLowerCase());
        FileHandler.queue.removeAll(FileHandler.queue.stream().filter(r -> r.database.equals(db))
                .collect(Collectors.toUnmodifiableList()));
        FileUtils.deleteDirectory(new File(String.format(RoseDB.directory + "/%s/", db)));
    }

    private static void register() {
        RoseListenerManager.register(new RequestListener());
        RoseListenerManager.register(new AddListener());
        RoseListenerManager.register(new DeleteListener());
        RoseListenerManager.register(new UpdateListener());
        RoseListenerManager.register(new DropListener());
        RoseListenerManager.register(new AggregateListener());
        RoseListenerManager.register(new RevertListener());
    }

    private static void startHeartbeat() {
        Scheduler.schedule(() -> context.values()
                .stream()
                .filter(wsContext -> wsContext.session.isOpen())
                .forEach(wsContext -> wsContext.send(new JSONObject()
                        .put("session", wsContext.getSessionId())
                        .put("kode", 0).toString())), RoseDB.heartbeat, RoseDB.heartbeat, TimeUnit.SECONDS);
        Terminal.log(Levels.DEBUG, "Heartbeat listener is now active.");
    }

    private static void startWriter(){
        Scheduler.schedule(FileHandler::write, 5, 5, TimeUnit.SECONDS);
    }

    public static void run(int port) {
        register();
        Terminal.log(Levels.DEBUG, "All listeners are registered.");

        if (Terminal.root == Level.DEBUG) {
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

            config.wsLogger(wsHandler -> {
                wsHandler.onConnect(wsConnectContext -> Terminal.log(Levels.DEBUG, "Received connection from {}", wsConnectContext.session.getRemoteAddress().toString()));
                wsHandler.onClose(wsCloseContext -> Terminal.log(Levels.DEBUG, "Received closed connection from {} for {}", wsCloseContext.session.getRemoteAddress().toString(), wsCloseContext.reason()));
                wsHandler.onMessage(ctx -> Terminal.log(Levels.DEBUG, "Received request: {} from {}", ctx.message(), ctx.session.getRemoteAddress().toString()));
            });
        });

        app.events(event -> {
            event.serverStarting(() -> Terminal.log(Levels.DEBUG, "The server is now starting at port: {}", port));
            event.serverStarted(() -> Terminal.log(Levels.DEBUG, "The server has started on port: {}", port));
            event.serverStartFailed(() -> Terminal.log(Levels.ERROR, "The server failed to start, possibly another instance at the same port is running."));
            event.serverStopping(() -> Terminal.log(Levels.INFO, "The server is now shutting off."));
            event.serverStopped(() -> Terminal.log(Levels.INFO, "The server has successfully closed."));
        }).start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> context.values().stream().filter(wsContext -> wsContext.session.isOpen())
        .forEachOrdered(wsContext -> wsContext.session.close(4002, "RoseDB is closing."))));

        if(RoseDB.versioning) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> database.asMap().values().forEach(roseDatabase -> roseDatabase.getCollections()
                    .forEach(collections -> collections.getVersions().forEach((s, s2) -> {
                        String location = new StringBuilder(".rose_versions")
                                .append(File.separator)
                                .append(roseDatabase.getDatabase())
                                .append(File.separator).append(collections.collection)
                                .append(File.separator).append(s)
                                .append(".rose").toString();

                        if (!new File(location).exists()) {
                            boolean mkdirs = new File(location).mkdirs();
                            if (!mkdirs) {
                                Terminal.setLoggingLevel(Level.ERROR);
                                Terminal.log(Levels.ERROR, "Failed to create folders for " + location + ", possibly we do not have permission to write.");
                            }
                        }

                        FileHandler.writeGzip(location, s2);
                    })))));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        Runtime.getRuntime().addShutdownHook(new Thread(FileHandler::write));
        Runtime.getRuntime().addShutdownHook(new Thread(Scheduler::shutdown));

        Terminal.log(Levels.DEBUG, "All events and handlers are now ready.");

        app.ws("/", ws -> {
            ws.onConnect(ctx -> {
                if(Optional.ofNullable(ctx.header("Authorization")).orElse("").equals(RoseDB.authorization))
                    context.put(ctx.getSessionId(), ctx);
                else
                    ctx.session.close(4001, "Missing or invalid Authorization header.");
            });
            ws.onClose(ctx -> context.remove(ctx.getSessionId()));
            ws.onMessage(ctx -> {
                try {
                    RoseListenerManager.execute(new JSONObject(ctx.message()), ctx);
                } catch (JSONException e) {
                    reply(ctx, "The request was considered as invalid: " + e.getMessage(), -1);
                    Terminal.log(Levels.DEBUG, "Received invalid JSON request: {} from {}", ctx.message(), ctx.session.getRemoteAddress().toString());
                } catch (MessageTooLargeException e){
                    reply(ctx, "The request was canceled by force: " + e.getMessage(), -1);
                    Terminal.log(Levels.ERROR, "Received message that was too large from {}", ctx.session.getRemoteAddress().toString());
                }
            });
        });

        Terminal.log(Levels.INFO, "RoseDB is now running on port: {}", port);
        startHeartbeat();
        startWriter();

        while(scanner.hasNextLine()){
            String request = scanner.nextLine();
            try {
                RoseListenerManager.execute(new JSONObject(request), null);
            } catch (JSONException e) {
                reply(null, "The request was considered as invalid: " + request, -1);
                Terminal.log(Levels.DEBUG, "Received invalid JSON request: {} from terminal", request);
            } catch (MessageTooLargeException e){
                reply(null, "The request was canceled by force: " + e.getMessage(), -1);
                Terminal.log(Levels.ERROR, "Received message that was too large from terminal.");
            }
        }
    }

}
