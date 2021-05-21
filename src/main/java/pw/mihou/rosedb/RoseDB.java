package pw.mihou.rosedb;

import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.utility.Terminal;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

public class RoseDB {

    public static int port = 5995;
    public static String directory;
    public static String authorization;

    public static void main(String[] args) throws URISyntaxException {
        if (!new File("config.json").exists()) {
            FileHandler.writeToFile("config.json",
                    new JSONObject()
                            .put("directory", new File(RoseDB.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI()
                                    .getPath())
                                    .getParentFile()
                                    .getPath() + "\\Database\\")
                            .put("port", port).put("authorization", UUID.randomUUID().toString())
                            .put("loggingLevel", "INFO").toString()).join();
        }

        JSONObject config = new JSONObject(FileHandler.read("config.json").join());

        port = config.isNull("port") ? 5995 : config.getInt("port");

        directory = Optional.ofNullable(config.getString("directory"))
                .orElse(new File(RoseDB.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
                        .getPath())
                        .getParentFile()
                        .getPath() + "\\Database\\");

        authorization = Optional.ofNullable(config.getString("authorization"))
                .orElse("null");

        Terminal.setLoggingLevel(rootLevel(Optional.ofNullable(config.getString("loggingLevel"))
                .orElse("INFO")));

        if (!new File(directory).exists())
            new File(directory).mkdirs();

        if (new File(directory).canWrite() && new File(directory).canRead()) {
            RoseServer.run(port);
        } else {
            Terminal.log(Levels.ERROR, "Rose cannot write on read or read on " + directory);
        }

    }

    public static Levels rootLevel(String configValue) {
        return configValue.equalsIgnoreCase("DEBUG") ? Levels.DEBUG : (configValue.equalsIgnoreCase("INFO") ? Levels.INFO :
                (configValue.equalsIgnoreCase("ERROR") ? Levels.ERROR : Levels.WARNING));
    }

}
