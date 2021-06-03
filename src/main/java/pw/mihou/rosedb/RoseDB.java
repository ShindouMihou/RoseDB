package pw.mihou.rosedb;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.utility.ColorPalette;
import pw.mihou.rosedb.utility.Terminal;
import pw.mihou.rosedb.utility.UpdateChecker;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
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
        String vanity = "\tr ______  ______  ______  ______  _____   ______    \n" +
                "\tb/\\  == \\/\\  __ \\/\\  ___\\/\\  ___\\/\\  __-./\\  == \\   \n" +
                "\ty\\ \\  __<\\ \\ \\/\\ \\ \\___  \\ \\  __\\\\ \\ \\/\\ \\ \\  __<   \n" +
                "\tc \\ \\_\\ \\_\\ \\_____\\/\\_____\\ \\_____\\ \\____-\\ \\_____\\ \n" +
                "\tr  \\/_/ /_/\\/_____/\\/_____/\\/_____/\\/____/ \\/_____/n";
        vanity = vanity.replaceAll("r", ColorPalette.ANSI_RED)
                .replaceAll("g", ColorPalette.ANSI_GREEN)
                .replaceAll("b", ColorPalette.ANSI_BLUE)
                .replaceAll("y", ColorPalette.ANSI_YELLOW)
                .replaceAll("c", ColorPalette.ANSI_CYAN)
                .replaceAll("n", ColorPalette.ANSI_RESET);
        System.out.println(vanity);
        System.out.printf("Version: %s, Build: %s, Configuration Version: %s, Creator: %s\n", UpdateChecker.VERSION, UpdateChecker.BUILD, UpdateChecker.CONFIG_VERSION, "Shindou Mihou");

        ((Logger) LoggerFactory.getLogger("io.javalin.Javalin")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org.eclipse.jetty.util.log")).setLevel(Level.ERROR);

        Terminal.setLoggingLevel(Level.ERROR);
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

            if(config.isNull("configVersion") || !config.isNull("configVersion") && Double.parseDouble(config.getString("configVersion")) < 1.2){
                FileHandler.setDirectory(config.isNull("directory") ?
                        new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                                .getParentFile().getPath() + File.separator + "Database" + File.separator : config.getString("directory"));
                Terminal.log(Levels.INFO, "Your configuration was detected to be from a version < 1.2 which meant the data files are utilizing an older format.");
                Terminal.log(Levels.INFO, "We will perform a short migration to the newer file format real quick... please do not close!");
                FileHandler.migrateAll().join();
            }

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
                    Terminal.log(Levels.ERROR, "We couldn't create the folders on {}", directory);
                }
            }

            if(config.getBoolean("updateChecker")){
                startUpdateChecker();
            }

            if (new File(directory).canWrite() && new File(directory).canRead()) {
                // We are moving original setting of root level here, we want errors to be logged for startup.
                Terminal.setLoggingLevel(rootLevel(Optional.ofNullable(config.getString("loggingLevel"))
                        .orElse("INFO")));

                FileHandler.setDirectory(directory);
                RoseServer.run(port);
            } else {
                Terminal.log(Levels.ERROR, "Rose cannot write on read or read on {}", directory);
            }
        } catch (JSONException | ArithmeticException e){
            Terminal.log(Levels.ERROR, "An error occurred, if this is sent from startup, " +
                    "please check config.json otherwise please send an issue at https://github.com/ShindouMihou/RoseDB/issues." +
                    "\nAvailable Processors (cores): {}, Free memory (MB): {}, Maximum Memory allocated to JVM (MB): {}, JDK Version: {}, JDK Vendor: {}, OS: {}, Architecture: {}, OS Version: {}" +
                            ".\n\nError: {}",
                    Runtime.getRuntime().availableProcessors(), (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1000 * 1000),
                    Runtime.getRuntime().totalMemory(), System.getProperty("java.vm.version"), System.getProperty("java.vm.vendor"),
                    System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"), e.getMessage());
        }

    }

    private static void startUpdateChecker(){
        Scheduler.schedule(() -> {
            if(UpdateChecker.check()){
                Terminal.log(Levels.INFO, "There is a newer version of RoseDB available, please update on https://github.com/ShindouMihou/RoseDB/releases");
            }
        }, 0, 12, TimeUnit.HOURS);
    }

    public static Level rootLevel(String configValue) {
        return configValue.equalsIgnoreCase("DEBUG") ? Level.DEBUG : (configValue.equalsIgnoreCase("INFO") ? Level.INFO :
                (configValue.equalsIgnoreCase("ERROR") ? Level.ERROR : Level.WARN));
    }

    private static JSONObject updateConfig(JSONObject original) throws URISyntaxException, JSONException {
        return new JSONObject().put("directory", Optional.ofNullable(original.getString("directory"))
                .orElse(new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath() + File.separator + "Database" + File.separator))
                .put("port", original.isNull("port") ? 5995 : original.getInt("port"))
                .put("authorization", Optional.ofNullable(original.getString("authorization")).orElse(UUID.randomUUID().toString()))
                .put("loggingLevel", Optional.ofNullable(original.getString("loggingLevel")).orElse("INFO"))
                .put("cores", original.isNull("cores") ? 1 : original.getInt("cores"))
                .put("updateChecker", original.isNull("updateChecker") || original.getBoolean("updateChecker"))
                .put("preload", true)
                .put("maxTextMessageBufferSizeMB", original.isNull("maxTextMessageBufferSizeMB") ? 5 : original.getInt("maxTextMessageBufferSizeMB"))
                .put("maxTextMessageSizeMB", original.isNull("maxTextMessageSizeMB") ? 5 : original.getInt("maxTextMessageSizeMB"))
                .put("heartbeatIntervalSeconds", original.isNull("heartbeatIntervalSeconds") ? 30 : original.getInt("heartbeatIntervalSeconds"))
                .put("configVersion", UpdateChecker.CONFIG_VERSION);
    }

}
