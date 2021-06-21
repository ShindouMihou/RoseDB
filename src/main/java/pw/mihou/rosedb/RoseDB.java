package pw.mihou.rosedb;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import journal.io.api.Journal;
import journal.io.api.JournalBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.readers.RoseReader;
import pw.mihou.rosedb.io.writers.RoseWriter;
import pw.mihou.rosedb.utility.ColorPalette;
import pw.mihou.rosedb.utility.RoseSSL;
import pw.mihou.rosedb.utility.Terminal;
import pw.mihou.rosedb.utility.UpdateChecker;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RoseDB {

    public static int port;
    public static String directory;
    public static byte[] authorization;
    public static byte[] secret;
    public static int cores;
    public static boolean preload;
    public static int buffer;
    public static int heartbeat;
    public static boolean versioning;
    public static int size;
    public static SSLContext sslContext;
    public static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    public static Retry retry = Retry.of("roseRetry", () -> RetryConfig.custom()
            .failAfterMaxAttempts(true).maxAttempts(500)
            .waitDuration(Duration.ofMillis(50))
            .retryExceptions(IOException.class)
            .build());
    public static TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
    .cancelRunningFuture(true).timeoutDuration(Duration.ofSeconds(30)).build());
    public static Journal journal;
    static {
        try {
            if(!new File(".journal").exists()) {
                if (!new File(".journal").mkdirs()) {
                    Terminal.setLoggingLevel(Level.ERROR);
                    Terminal.log(Levels.ERROR, "Failed to create .journal folder, possibly we do not have permission to write.");
                }
            }

            journal = JournalBuilder.of(new File(".journal")).setChecksum(true).open();
        } catch (IOException exception) {
            Terminal.log(Levels.ERROR, "We couldn't open up the journal because {}, closing application...", exception.getMessage());
            System.exit(0);
        }
    }
    /**
     * The main method which is ran at the very start of the application.
     * @param args The arguments from the JVM.
     * @throws Exception A random exception.
     */
    public static void main(String[] args) throws Exception {
        System.out.println(vanity.replaceAll("r", ColorPalette.ANSI_RED)
                .replaceAll("g", ColorPalette.ANSI_GREEN)
                .replaceAll("b", ColorPalette.ANSI_BLUE)
                .replaceAll("y", ColorPalette.ANSI_YELLOW)
                .replaceAll("c", ColorPalette.ANSI_CYAN)
                .replaceAll("n", ColorPalette.ANSI_RESET));
        System.out.printf("Version: %s, Build: %s, Configuration Version: %s, Creator: %s\n", UpdateChecker.VERSION, UpdateChecker.BUILD, UpdateChecker.CONFIG_VERSION, "Shindou Mihou");

        disableAllExternalLogging();
        Terminal.setLoggingLevel(Level.ERROR);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                journal.close();
            } catch (IOException exception) {
                Terminal.log(Levels.ERROR, "Attempt to close journal was met with {}", exception.getMessage());
            }
        }));

        if (!new File("config.json").exists()) {
            RoseWriter.write("config.json", defaultConfig().toString(4));
        }

        JSONObject config = new JSONObject(RoseReader.read("config.json").join());
        if (config.isNull("configVersion") || !config.getString("configVersion").equals(UpdateChecker.CONFIG_VERSION)) {

            if(config.isNull("configVersion") || !config.isNull("configVersion") && Double.parseDouble(config.getString("configVersion")) < 1.2){
                FileHandler.setDirectory(config.isNull("directory") ?
                        new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                                .getParentFile().getPath() + File.separator + "Database" + File.separator : config.getString("directory"));
                Terminal.log(Levels.INFO, "Your configuration was detected to be from a version < 1.2 which meant the data files are utilizing an older format.");
                Terminal.log(Levels.INFO, "We will perform a short migration to the newer file format real quick... please do not close!");
                FileHandler.migrateAll();
            }

            Terminal.log(Levels.INFO, "We have noticed that your config.json is outdated, we are currently going to perform a short configuration update.");
            Terminal.log(Levels.INFO, "Don't worry, there isn't anything you need to do on your side!");
            RoseWriter.write("config.json", updateConfig(config).toString(4));
            config = new JSONObject(RoseReader.read("config.json").join());
            Terminal.log(Levels.INFO, "Configuration update was complete.");
        }

        try {
            port = config.getInt("port");
            cores = config.getInt("cores");
            directory = config.getString("directory");
            preload = config.getBoolean("preload");
            buffer = config.getInt("maxTextMessageBufferSizeMB");
            size = config.getInt("maxTextMessageSizeMB");
            heartbeat = config.getInt("heartbeatIntervalSeconds");
            versioning = config.getBoolean("versioning");

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

            if(new File("ssl.json").exists()){
                JSONObject sslConf = new JSONObject(RoseReader.read("ssl.json").join());

                if(sslConf.isNull("storeType")) {
                    if (sslConf.isNull("pathToPEM")) {
                        Terminal.log(Levels.ERROR, "Path to SSL does not exist, [pathToPEM] on ssl.json needs a value.");
                        return;
                    }

                    if (sslConf.isNull("keyPassword")) {
                        Terminal.log(Levels.ERROR, "Key Password for SSL does not exist, [keyPassword] on ssl.json needs a value.");
                        return;
                    }

                    sslContext = RoseSSL.getSSLContextFromLetsEncrypt(sslConf.getString("pathToPEM"), sslConf.getString("keyPassword"));
                } else {
                    // String storeType, String keystore, String storePassword, String keyPassword
                    if(sslConf.isNull("keyStore")){
                        Terminal.log(Levels.ERROR, "SSL Keystore does not exist, [keyStore] on ssl.json needs a value.");
                        return;
                    }

                    if(sslConf.isNull("storePassword")){
                        Terminal.log(Levels.ERROR, "SSL Store Password does not exist, [storePassword] on ssl.json needs a value.");
                        return;
                    }

                    if(sslConf.isNull("keyPassword")){
                        Terminal.log(Levels.ERROR, "Key Password does not exist, [keyPassword] on ssl.json needs a value.");
                        return;
                    }

                    sslContext = RoseSSL.getSSLConextFromKeystore(sslConf.getString("storeType"), sslConf.getString("keyStore"),
                            sslConf.getString("storePassword"), sslConf.getString("keyPassword"));
                }
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

                Scanner scan = new Scanner(System.in);
                if(!new File(FileHandler.format(".rose_secrets", ".rose_heart", "passwd")).exists()){
                    byte[] secret = UUID.randomUUID().toString().replaceAll("-", "").getBytes();
                    String authorization;
                    if(config.isNull("authorization")){
                        System.out.println("It seems like this is your first installation of RoseDB, welcome!");
                        System.out.print("To start, please set your secure Authorization token for the application "
                                +ColorPalette.ANSI_RED+"(be sure to save it!): "+ColorPalette.ANSI_RESET);


                        String s = scan.nextLine();
                        authorization = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(s);
                    } else {
                        System.out.println("It seems like you are migrating from an older version of RoseDB, welcome!");
                        System.out.print("Since you are migrating, we are allowing you to pick whether to continue using your old authorization token " +
                                "or change it? (Y to reuse/N to create new): ");
                        if(scan.nextLine().equalsIgnoreCase("y")){
                            authorization = new String(DigestUtils.sha256(config.getString("authorization")));
                        } else {
                            System.out.print("To start, please set your secure Authorization token for the application "
                                    +ColorPalette.ANSI_RED+"(be sure to save it!): "+ColorPalette.ANSI_RESET);

                            String s = scan.nextLine();
                            authorization = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(s);
                        }

                        RoseWriter.write("config.json", updateConfig(config).toString(4));
                    }

                    RoseWriter.write(".rose_secrets", ".rose_heart", "passwd", authorization);
                    RoseWriter.write(".rose_secrets", ".rose_heart", "sec", new String(secret));
                    RoseWriter.write();

                    System.out.println("RoseDB will be closing, please reopen for security reasons.");
                    System.exit(0);
                } else {
                    authorization = FileHandler.readData(".rose_secrets", ".rose_heart", "passwd")
                            .map(String::getBytes)
                            .orElseThrow(() -> new Exception("We couldn't read the hash token!"));
                    secret = FileHandler.readData(".rose_secrets", ".rose_heart", "sec")
                            .map(String::getBytes).orElseThrow(() -> new Exception("We couldn't read the hash secret."));
                }

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

    /**
     * Starts the update checking towards the API.
     * This usually checks every 12 hours to prevent spam requests towards
     * both the API and reduce the network usage of the host server.
     */
    private static void startUpdateChecker(){
        if(!UpdateChecker.BUILD.contains("DEV") || !UpdateChecker.BUILD.contains("SNAPSHOT")) {
            Scheduler.schedule(() -> {
                if (UpdateChecker.check()) {
                    Terminal.log(Levels.INFO, "There is a newer version of RoseDB available, please update on https://github.com/ShindouMihou/RoseDB/releases");
                }
            }, 0, 12, TimeUnit.HOURS);
        } else {
            Terminal.log(Levels.INFO, "You are running a SNAPSHOT (DEV) version of RoseDB, update checking has been disabled...");
        }
    }

    /**
     * Identifies the root level set on the configuration file.
     *
     * @param configValue The value on the configuration file.
     * @return The identified from the string value on the configuration file, defaults to INFO.
     */
    public static Level rootLevel(String configValue) {
        return configValue.equalsIgnoreCase("DEBUG") ? Level.DEBUG : (configValue.equalsIgnoreCase("WARNING") ? Level.WARN :
                (configValue.equalsIgnoreCase("ERROR") ? Level.ERROR : Level.INFO));
    }

    /**
     * Moves all the old values of the older configuration that the host
     * has and upgrades them all by adding all the values that doesn't exist.
     *
     * @param original The original configuration object.
     * @return The new configuration in the form of a JSON Object.
     * @throws URISyntaxException This is thrown if the syntax of the URI is wrong.
     * @throws JSONException This is thrown if a value inside the old configuration doesn't match the standard.
     */
    private static JSONObject updateConfig(JSONObject original) throws URISyntaxException, JSONException {
        return new JSONObject().put("directory", Optional.ofNullable(original.getString("directory"))
                .orElse(new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath() + File.separator + "Database" + File.separator))
                .put("port", original.isNull("port") ? 5995 : original.getInt("port"))
                .put("loggingLevel", Optional.ofNullable(original.getString("loggingLevel")).orElse("INFO"))
                .put("cores", original.isNull("cores") ? 1 : original.getInt("cores"))
                .put("updateChecker", original.isNull("updateChecker") || original.getBoolean("updateChecker"))
                .put("preload", original.isNull("preload") || original.getBoolean("preload"))
                .put("versioning", original.isNull("versioning") || original.getBoolean("versioning"))
                .put("maxTextMessageBufferSizeMB", original.isNull("maxTextMessageBufferSizeMB") ? 5 : original.getInt("maxTextMessageBufferSizeMB"))
                .put("maxTextMessageSizeMB", original.isNull("maxTextMessageSizeMB") ? 5 : original.getInt("maxTextMessageSizeMB"))
                .put("heartbeatIntervalSeconds", original.isNull("heartbeatIntervalSeconds") ? 30 : original.getInt("heartbeatIntervalSeconds"))
                .put("configVersion", UpdateChecker.CONFIG_VERSION);
    }

    /**
     * The default configuration of the application,
     * you may add new values here if you need it to persist.
     * Any new values must also be added onto updateConfig();
     *
     * @return The default configuration as a JSON Object.
     * @throws URISyntaxException This is thrown if the syntax of the URI is wrong.
     */
    private static JSONObject defaultConfig() throws URISyntaxException {
        return new JSONObject()
                .put("directory", new File(RoseDB.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath() + File.separator + "Database" + File.separator)
                .put("port", 5995)
                .put("loggingLevel", "INFO")
                .put("cores", 1)
                .put("updateChecker", true)
                .put("preload", true)
                .put("maxTextMessageBufferSizeMB", 5)
                .put("maxTextMessageSizeMB", 5)
                .put("versioning", true)
                .put("heartbeatIntervalSeconds", 30)
                .put("configVersion", UpdateChecker.CONFIG_VERSION);
    }

    /**
     * Disables all external logging, this caused
     * older versions of RoseDB to be filled with hard
     * to read logs.
     */
    private static void disableAllExternalLogging(){
        ((Logger) LoggerFactory.getLogger("io.javalin.Javalin")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org.eclipse.jetty.util.log")).setLevel(Level.ERROR);
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        System.setProperty("org.eclipse.jetty.util.log.announce", "false");
        ((Logger) LoggerFactory.getLogger("io.javalin.Javalin")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org.eclipse.jetty.util.log")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org.eclipse.jetty")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("o.e.jetty.io")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("o.e.j.w.common")).setLevel(Level.ERROR);
    }

    /**
     * The default vanity for RoseDB.
     * Please do not change this unless you are making
     * your own custom build of the application.
     */
    private static final String vanity = "\tr ______  ______  ______  ______  _____   ______    \n" +
            "\tb/\\  == \\/\\  __ \\/\\  ___\\/\\  ___\\/\\  __-./\\  == \\   \n" +
            "\ty\\ \\  __<\\ \\ \\/\\ \\ \\___  \\ \\  __\\\\ \\ \\/\\ \\ \\  __<   \n" +
            "\tc \\ \\_\\ \\_\\ \\_____\\/\\_____\\ \\_____\\ \\____-\\ \\_____\\ \n" +
            "\tr  \\/_/ /_/\\/_____/\\/_____/\\/_____/\\/____/ \\/_____/n";

}
