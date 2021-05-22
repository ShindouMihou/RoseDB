package pw.mihou.rosedb.utility;

import pw.mihou.rosedb.enums.Levels;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Terminal {

    private static final String format = "[%s] [RoseDB: %s] > %s\n";
    public static Levels root = Levels.ERROR;

    public static void setLoggingLevel(Levels level) {
        root = level;
    }

    private static String getTime() {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());
    }

    public static void log(Levels level, String message) {
        if (level.id <= root.id) {
            System.out.printf(format, getTime(), level.name, message);
        }
    }

}
