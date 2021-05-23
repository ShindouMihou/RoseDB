package pw.mihou.rosedb;

import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.utility.Terminal;
import pw.mihou.rosedb.utility.UpdateChecker;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RoseDB {

    public static int port = 5995;
    public static String directory;
    public static String authorization;
    public static int cores = 1;

    public static void main(String[] args) throws URISyntaxException {

        Terminal.setLoggingLevel(Levels.ERROR);
        if (!new File("config.json").exists()) {
            FileHandler.writeToFile("config.json", new JSONObject()
                    .put("directory", new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath() + File.separator + "Database" + File.separator)
                    .put("port", port)
                    .put("authorization", UUID.randomUUID().toString())
                    .put("loggingLevel", "INFO")
                    .put("cores", 1)
                    .put("updateChecker", true)
                    .put("configVersion", UpdateChecker.CONFIG_VERSION)
                    .toString()).join();
        }

        JSONObject config = new JSONObject(FileHandler.read("config.json").join());

        if (config.isNull("configVersion") || !config.getString("configVersion").equals(UpdateChecker.CONFIG_VERSION)) {
            Terminal.log(Levels.INFO, "We have noticed that your config.json is outdated, we are currently going to perform a short configuration update.");
            Terminal.log(Levels.INFO, "Don't worry, there isn't anything you need to do on your side!");
            FileHandler.writeToFile("config.json", updateConfig(config).toString()).join();
            Terminal.log(Levels.INFO, "Configuration update was complete.");
        }

        try {
            port = config.getInt("port");
            cores = config.getInt("cores");
            directory = config.getString("directory");
            authorization = config.getString("authorization");

            if (!new File(directory).exists()) {
                boolean mkdirs = new File(directory).mkdirs();

                if(!mkdirs){
                    Terminal.log(Levels.ERROR, "We couldn't create the folders on " + directory);
                }
            }

            if(config.getBoolean("updateChecker")){
                startUpdateChecker();
            }

            if (new File(directory).canWrite() && new File(directory).canRead()) {
                // We are moving original setting of root level here, we want errors to be logged for startup.
                Terminal.setLoggingLevel(rootLevel(Optional.ofNullable(config.getString("loggingLevel"))
                        .orElse("INFO")));
                RoseServer.run(port);

            } else {
                Terminal.log(Levels.ERROR, "Rose cannot write on read or read on " + directory);
            }
        } catch (JSONException e){
            Terminal.log(Levels.ERROR, e.getMessage());
            Terminal.log(Levels.ERROR, "An error from this side is caused by a misconfiguration of config.json, please fix your config.json.");
        }

    }

    private static void startUpdateChecker(){
        Scheduler.schedule(() -> {
            boolean update = UpdateChecker.check();
            if(update){
                Terminal.log(Levels.INFO, "There is a newer version of RoseDB available, please update on https://github.com/ShindouMihou/RoseDB/releases");
            }
        }, 0, 12, TimeUnit.HOURS);
    }

    public static Levels rootLevel(String configValue) {
        return configValue.equalsIgnoreCase("DEBUG") ? Levels.DEBUG : (configValue.equalsIgnoreCase("INFO") ? Levels.INFO :
                (configValue.equalsIgnoreCase("ERROR") ? Levels.ERROR : Levels.WARNING));
    }

    private static JSONObject updateConfig(JSONObject original) throws URISyntaxException, JSONException {
        return new JSONObject().put("directory", Optional.ofNullable(original.getString("directory"))
                .orElse(new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath() + File.separator + "Database" + File.separator))
                .put("port", original.isNull("port") ? 5995 : original.getInt("port"))
                .put("authorization", Optional.ofNullable(original.getString("authorization")).orElse(UUID.randomUUID().toString()))
                .put("loggingLevel", Optional.ofNullable(original.getString("loggingLevel")).orElse("INFO"))
                .put("cores", original.isNull("cores") ? 1 : original.getInt("cores"))
                .put("updateChecker", original.isNull("updateChecker") ? true : original.getBoolean("updateChecker"))
                .put("configVersion", UpdateChecker.CONFIG_VERSION);
    }

}
