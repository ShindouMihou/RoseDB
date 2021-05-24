package pw.mihou.rosedb.utility;

import pw.mihou.rosedb.enums.Levels;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

    // Config Version is used to validate for configuration changes.
    // This will not be checked through the online version and will instead be
    // done on the application itself.
    public static final String CONFIG_VERSION = "1.2";
    private static final String ENCODED_VERSION = URLEncoder.encode("1.0.7", StandardCharsets.UTF_8);

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
