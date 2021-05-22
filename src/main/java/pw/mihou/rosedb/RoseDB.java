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
    public static int cores = 1;

    public static void main(String[] args) throws URISyntaxException {
        String defaultPath = new StringBuilder(new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                .getParentFile().getPath()).append(File.separator).append("Database").append(File.separator).toString();

        if (!new File("config.json").exists()) {
            FileHandler.writeToFile("config.json", new JSONObject()
                    .put("directory", defaultPath)
                    .put("port", port).put("authorization", UUID.randomUUID().toString())
                    .put("loggingLevel", "INFO").put("cores", 1).toString()).join();
        }

        JSONObject config = new JSONObject(FileHandler.read("config.json").join());

        port = config.isNull("port") ? 5995 : config.getInt("port");

        cores = config.isNull("cores") ? 1 : config.getInt("cores");

        directory = Optional.ofNullable(config.getString("directory")).orElse(defaultPath);

        authorization = Optional.ofNullable(config.getString("authorization"))
                .orElse("null");

        Terminal.setLoggingLevel(rootLevel(Optional.ofNullable(config.getString("loggingLevel"))
                .orElse("INFO")));

        if (!new File(directory).exists())
            new File(directory).mkdirs();

        if (new File(directory).canWrite() && new File(directory).canRead()) {
            // There is a reason why we don't preload, especially for databases that are very large.
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
