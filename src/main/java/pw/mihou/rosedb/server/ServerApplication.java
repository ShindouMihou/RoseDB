package pw.mihou.rosedb.server;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.protocols.Protocol;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.RoseListenerManager;
import pw.mihou.rosedb.utility.Terminal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.UUID;

public class ServerApplication extends WebSocketServer {

    /**
     * Creates the server for the application.
     *
     * @param port The port to run the server on.
     */
    public ServerApplication(int port){
        super(new InetSocketAddress(port), Collections.singletonList(new Draft_6455(Collections.<IExtension>emptyList(),
                Collections.singletonList(new Protocol("")),
                RoseDB.size * 1024 * 1024)));

        if(RoseDB.sslContext != null)
            setWebSocketFactory(new DefaultSSLWebSocketServerFactory(RoseDB.sslContext));
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        RoseServer.context.values().stream().filter(WebSocket::isOpen)
                .forEachOrdered(wsContext -> wsContext.close(4002, "RoseDB is closing."));
        super.stop();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Terminal.log(Levels.DEBUG, "Received attempt connection from {}", conn.getRemoteSocketAddress().toString());
        if(!handshake.hasFieldValue("Authorization")){
            conn.close(4001, "Missing or invalid Authorization header.");
            return;
        }

        if (MessageDigest.isEqual(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, RoseDB.secret)
                .hmacHex(handshake.getFieldValue("Authorization")).getBytes(), RoseDB.authorization)){
            conn.setAttachment(UUID.randomUUID().toString().replaceAll("-", ""));
            RoseServer.context.put(conn.getAttachment(), conn);
            Terminal.log(Levels.DEBUG, "{} has successfully been connected with ({}) UUID attached.", conn.getRemoteSocketAddress().toString(), conn.getAttachment());
        } else {
            conn.close(4001, "Missing or invalid Authorization header.");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Terminal.log.debug("Received code ({}) close connection from {} with reason {}", code, conn.getAttachment(),
                reason);

        if(conn.getAttachment() != null && conn.getAttachment() instanceof String)
            RoseServer.context.remove((String) conn.getAttachment());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if((conn.getAttachment() == null || !(conn.getAttachment() instanceof String)) || (conn.getAttachment() != null && !(conn.getAttachment() instanceof String)
        && !RoseServer.context.containsKey((String) conn.getAttachment()))){
            Terminal.log.warn("A client attempted to connect without valid UUID, kicking from connections.");
            conn.close(4001, "The client is not identified by the server.");
            return;
        }

        QueryRequest req = RoseQuery.parse(message);
        try {
            if(!req.isValid())
                RoseListenerManager.execute(new JSONObject(message), conn);
            else
                RoseListenerManager.execute(req, conn);
        } catch (JSONException e) {
            RoseServer.reply(conn, "The request was considered as invalid: " + e.getMessage(), -1);
            Terminal.log(Levels.DEBUG, "Received invalid JSON request: {} from {}", message, conn.getRemoteSocketAddress().toString());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if(conn != null && conn.isOpen()) {
            RoseServer.reply(conn, "An exception occurred: " + ex.getMessage(), -1);
            Terminal.log(Levels.WARNING, "The websocket server returned an exception for remote client {} with reason {}", conn.getAttachment(), ex.getMessage());
        } else {
            Terminal.log(Levels.WARNING, "The websocket server returned an exception for a remote client with reason {}", ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        Terminal.log(Levels.INFO, "The server is now running at specified port.");
    }
}
