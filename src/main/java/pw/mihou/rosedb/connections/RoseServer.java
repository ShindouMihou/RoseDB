package pw.mihou.rosedb.connections;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.apache.commons.io.FileUtils;
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

    public static void reply(WsContext context, String response, int kode){
        context.send(new JSONObject().put("response", response).put("kode", kode).toString());
    }

    public static RoseDatabase getDatabase(String db){
        if(!database.containsKey(db.toLowerCase()))
            database.put(db.toLowerCase(), FileHandler.readDatabase(db.toLowerCase()));

        return database.get(db.toLowerCase());
    }

    public static void removeDatabase(String db) throws IOException {
        database.remove(db.toLowerCase());
        FileUtils.deleteDirectory(new File(String.format(RoseDB.directory+"/%s/", db)));
    }

    private static void register(){
        RoseListenerManager.register(new RequestListener());
        RoseListenerManager.register(new AddListener());
        RoseListenerManager.register(new DeleteListener());
        RoseListenerManager.register(new UpdateListener());
        RoseListenerManager.register(new DropListener());
    }

    private static void startHeartbeat(){
        Scheduler.schedule(() -> context.values()
                .stream()
                .filter(wsContext -> wsContext.session.isOpen())
                .forEach(wsContext -> wsContext.send(new JSONObject()
                        .put("session", wsContext.getSessionId())
                        .put("kode", 0).toString())), 15, 15, TimeUnit.SECONDS);
        Terminal.log(Levels.DEBUG, "Heartbeat listener is now active.");
    }

    public static void run(int port){
        register();

        System.out.println(" ______  ______  ______  ______  _____   ______    \n" +
                "/\\  == \\/\\  __ \\/\\  ___\\/\\  ___\\/\\  __-./\\  == \\   \n" +
                "\\ \\  __<\\ \\ \\/\\ \\ \\___  \\ \\  __\\\\ \\ \\/\\ \\ \\  __<   \n" +
                " \\ \\_\\ \\_\\ \\_____\\/\\_____\\ \\_____\\ \\____-\\ \\_____\\ \n" +
                "  \\/_/ /_/\\/_____/\\/_____/\\/_____/\\/____/ \\/_____/");
        Terminal.log(Levels.DEBUG, "All listeners are registered.");

        Javalin app = Javalin.create(config -> config.wsLogger(wsHandler -> {
            wsHandler.onConnect(wsConnectContext -> Terminal.log(Levels.DEBUG, "Received connection from " + wsConnectContext.session.getRemoteAddress().toString()));
            wsHandler.onClose(wsCloseContext -> Terminal.log(Levels.DEBUG, "Received closed connection from " + wsCloseContext.session.getRemoteAddress().toString() + " for " + wsCloseContext.reason()));
            wsHandler.onMessage(ctx -> Terminal.log(Levels.DEBUG, "Received request: " + ctx.message() + " from " + ctx.session.getRemoteAddress().toString()));
        })).events(event -> {
            event.serverStarting(() -> Terminal.log(Levels.DEBUG, "The server is now starting at port: " + port));
            event.serverStarted(() -> Terminal.log(Levels.DEBUG, "The server has started on port: " + port));
            event.serverStartFailed(() -> Terminal.log(Levels.ERROR, "The server failed to start, possibly another instance at the same port is running."));
            event.serverStopping(() -> Terminal.log(Levels.INFO, "The server is now shutting off."));
            event.serverStopped(() -> Terminal.log(Levels.INFO, "The server has successfully closed."));
        }).start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        Terminal.log(Levels.DEBUG, "All events and handlers are now ready.");

        app.ws("/" , ws -> {
           ws.onConnect(ctx -> context.put(ctx.getSessionId(), ctx));
           ws.onClose(ctx -> context.remove(ctx.getSessionId()));
           ws.onMessage(ctx -> RoseListenerManager.execute(new JSONObject(ctx.message()), ctx));
        });

        Terminal.log(Levels.INFO, "RoseDB is now running on port: " + port);
        startHeartbeat();
    }

}
