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

    public static int port;
    public static String directory;
    public static String authorization;
    public static int cores;
    public static boolean preload;
    public static int buffer;
    public static int heartbeat;
    public static int size;

    public static void main(String[] args) throws URISyntaxException {
        System.out.println(" ______  ______  ______  ______  _____   ______    \n" +
                "/\\  == \\/\\  __ \\/\\  ___\\/\\  ___\\/\\  __-./\\  == \\   \n" +
                "\\ \\  __<\\ \\ \\/\\ \\ \\___  \\ \\  __\\\\ \\ \\/\\ \\ \\  __<   \n" +
                " \\ \\_\\ \\_\\ \\_____\\/\\_____\\ \\_____\\ \\____-\\ \\_____\\ \n" +
                "  \\/_/ /_/\\/_____/\\/_____/\\/_____/\\/____/ \\/_____/");

        Terminal.setLoggingLevel(Levels.ERROR);
        if (!new File("config.json").exists()) {
            FileHandler.writeToFile("config.json", new JSONObject()
                    .put("directory", new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath() + File.separator + "Database" + File.separator)
                    .put("port", port)
                    .put("authorization", UUID.randomUUID().toString())
                    .put("loggingLevel", "INFO")
                    .put("cores", 1)
                    .put("updateChecker", true)
                    .put("preload", true)
                    .put("maxTextMessageBufferSizeMB", 5)
                    .put("maxTextMessageSizeMB", 5)
                    .put("heartbeatIntervalSeconds", 30)
                    .put("configVersion", UpdateChecker.CONFIG_VERSION)
                    .toString()).join();
        }

        JSONObject config = new JSONObject(FileHandler.read("config.json").join());

        if (config.isNull("configVersion") || !config.getString("configVersion").equals(UpdateChecker.CONFIG_VERSION)) {
            Terminal.log(Levels.INFO, "We have noticed that your config.json is outdated, we are currently going to perform a short configuration update.");
            Terminal.log(Levels.INFO, "Don't worry, there isn't anything you need to do on your side!");
            FileHandler.writeToFile("config.json", updateConfig(config).toString()).join();
            config = new JSONObject(FileHandler.read("config.json").join());
            Terminal.log(Levels.INFO, "Configuration update was complete.");
        }

        try {
            port = config.getInt("port");
            cores = config.getInt("cores");
            directory = config.getString("directory");
            authorization = config.getString("authorization");
            preload = config.getBoolean("preload");
            buffer = config.getInt("maxTextMessageBufferSizeMB");
            size = config.getInt("maxTextMessageSizeMB");
            heartbeat = config.getInt("heartbeatIntervalSeconds");

            if(heartbeat > 300 || heartbeat < 25){
                Terminal.log(Levels.ERROR, "Minimum heartbeat interval should be at 25 seconds to prevent overloading clients.");
                Terminal.log(Levels.ERROR, "Maximum heartbeat interval should be at 300 seconds to prevent clients from timeout.");
                Terminal.log(Levels.ERROR, "Please change the value on config.json, we recommend keeping it within 25-300 seconds.");
                return;
            }

            if(buffer > 1024 || size > 1024){
                Terminal.log(Levels.ERROR, "Maximum buffer and size for text message must not exceed 1024 MB.");
                Terminal.log(Levels.ERROR, "Also, another warning, RoseDB isn't supposed to be used for handling large files.");
                Terminal.log(Levels.ERROR, "Please change the value on config.json, we recommend keeping it at 5 MB to 12 MB.");
                return;
            }

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
        } catch (ArithmeticException e){
            Terminal.log(Levels.ERROR, e.getMessage());
            Terminal.log(Levels.ERROR, "If this exception was thrown at the start, please check your config.json whether everything meets 32 bit integer limit.");
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
                .put("preload", true)
                .put("maxTextMessageBufferSizeMB", original.isNull("maxTextMessageBufferSizeMB") ? 5 : original.getInt("maxTextMessageBufferSizeMB"))
                .put("maxTextMessageSizeMB", original.isNull("maxTextMessageSizeMB") ? 5 : original.getInt("maxTextMessageSizeMB"))
                .put("heartbeatIntervalSeconds", original.isNull("heartbeatIntervalSeconds") ? 30 : original.getInt("heartbeatIntervalSeconds"))
                .put("configVersion", UpdateChecker.CONFIG_VERSION);
    }

}
