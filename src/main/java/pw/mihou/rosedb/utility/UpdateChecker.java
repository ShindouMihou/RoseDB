package pw.mihou.rosedb.utility;

import pw.mihou.rosedb.enums.Levels;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

    /**
     * This is used to validate the configuration changes.
     * This will not be checked through the online version but
     * will be done at an application level.
     */
    public static final String CONFIG_VERSION = "1.2";

    /**
     * The current version of the application.
     * Please increment this along with pom.xml's version.
     */
    public static final String VERSION = "1.2.0";

    /**
     * The BUILD version of the application.
     * Please change this along with the pom.xml's value.
     */
    public static final String BUILD = "DEV-SNAPSHOT";

    /**
     * The URL-encoded version of the application version.
     * This is the value that will be sent to the API.
     */
    private static final String ENCODED_VERSION = URLEncoder.encode(VERSION, StandardCharsets.UTF_8);

    /**
     * Checks whether there is a recent update of RoseDB.
     *
     * @return Whether there is a new update of RoseDB.
     */
    public static boolean check(){
        try {
            HttpURLConnection url = (HttpURLConnection) new URL(new StringBuilder("https://cdn.mihou.pw/rosedb/?update=")
                    .append(ENCODED_VERSION).toString()).openConnection();

            url.setRequestMethod("GET");
            url.connect();

            return url.getResponseCode() == 200;

        } catch (IOException exception) {
            Terminal.log(Levels.WARNING,
                    "Failed to connect to update check endpoint, if you are running offline mode, please disable update checker on config.json");
        }
        return false;
    }

}
